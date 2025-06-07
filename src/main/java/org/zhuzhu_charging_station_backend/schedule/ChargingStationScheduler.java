package org.zhuzhu_charging_station_backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zhuzhu_charging_station_backend.entity.*;
import org.zhuzhu_charging_station_backend.repository.ChargingStationRepository;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChargingStationScheduler {

    private final ChargingStationRepository chargingStationRepository;
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Order> orderRedisTemplate;
    private final RedisTemplate<String, ChargingStationSlot> slotRedisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    private static final String FAST_QUEUE_KEY = "queue:fast";
    private static final String SLOW_QUEUE_KEY = "queue:slow";
    private static final String ORDER_KEY_PREFIX = "order:";
    private static final String SLOT_KEY_PREFIX = "slot:";

    @Scheduled(cron = "*/1 * * * * *") // 每秒执行一次
    public void chargingStationTask() {
        List<ChargingStation> stations = chargingStationRepository.findAll();
        for (ChargingStation station : stations) {
            try {
                ChargingStationSlot slot = slotRedisTemplate.opsForValue().get(SLOT_KEY_PREFIX + station.getId());
                if (slot == null) continue;
                if (slot.getStatus() == null || slot.getStatus().getStatus() == 2 || slot.getStatus().getStatus() == 3) {
                    // 桩关闭或故障，不处理
                    continue;
                }
                processQueueFill(station, slot);
                processChargingHeadOrder(station, slot);
                // 保存slot到redis
                slotRedisTemplate.opsForValue().set(SLOT_KEY_PREFIX + station.getId(), slot);
            } catch (Exception e) {
                log.error("调度充电桩[" + station.getId() + "]异常", e);
            }
        }
    }

    // 补全队列
    private void processQueueFill(ChargingStation station, ChargingStationSlot slot) {
        int needFill = station.getMaxQueueLength() - (slot.getQueue() == null ? 0 : slot.getQueue().size());
        if (needFill <= 0) return;
        String queueKey = station.getMode() == 1 ? FAST_QUEUE_KEY : SLOW_QUEUE_KEY;
        Set<String> waitingOrderIds = stringRedisTemplate.opsForSet().members(queueKey);
        if (waitingOrderIds == null) return;
        if (slot.getQueue() == null) slot.setQueue(new ArrayList<>());

        for (String orderIdStr : waitingOrderIds) {
            if (slot.getQueue().size() >= station.getMaxQueueLength()) break;
            Long orderId = null;
            try { orderId = Long.parseLong(orderIdStr); } catch (Exception ignore) {}
            if (orderId == null || slot.getQueue().contains(orderId)) continue;
            Order order = orderRedisTemplate.opsForValue().get(ORDER_KEY_PREFIX + orderId);
            if (order == null) continue;
            if (order.getStatus() == 3) { // 3=等待中
                order.setStatus(2); // 2=排队中
                order.setQueueNo(null);
                orderRedisTemplate.opsForValue().set(ORDER_KEY_PREFIX + orderId, order);
                stringRedisTemplate.opsForSet().remove(queueKey, orderIdStr);
                slot.getQueue().add(orderId);
            }
        }
    }

    // 处理队首订单
    private void processChargingHeadOrder(ChargingStation station, ChargingStationSlot slot) {
        if (slot.getQueue() == null || slot.getQueue().isEmpty()) return;
        Long orderId = slot.getQueue().get(0);
        Order order = orderRedisTemplate.opsForValue().get(ORDER_KEY_PREFIX + orderId);
        if (order == null) {
            slot.getQueue().remove(0); // 移除无效单
            return;
        }

        if (order.getStatus() != 1) { // 非进行中，初始化
            order.setStatus(1);
            order.setActualCharge(0.0);
            order.setChargeDuration(0L);
            order.setChargeFee(0.0);
            order.setServiceFee(0.0);
            order.setTotalFee(0.0);
            order.setStartTime(LocalDateTime.now());
        }

        // 每秒推进
        order.setChargeDuration(order.getChargeDuration() + 1);
        double addedCharge = station.getPower() / 3600.0; // 以度（kwh）为单位/每秒
        order.setActualCharge(order.getActualCharge() + addedCharge);

        double unitPrice = calcUnitPrice(LocalTime.now(), station.getUnitPrices());
        order.setChargeFee(order.getChargeFee() + unitPrice * addedCharge);
        double addedService = station.getServiceFee() / 3600.0;
        order.setServiceFee(order.getServiceFee() + addedService);
        order.setTotalFee(order.getChargeFee() + order.getServiceFee());

        orderRedisTemplate.opsForValue().set(ORDER_KEY_PREFIX + orderId, order);

        // 判断是否充满
        if (order.getActualCharge() >= order.getChargeAmount()) {
            LocalDateTime stop = LocalDateTime.now();
            order.setStopTime(stop);
            order.setStatus(0); // 已完成
            orderRedisTemplate.delete(ORDER_KEY_PREFIX + orderId);
            orderRepository.save(order);
            slot.getQueue().remove(0);
            // 发消息/推送/断开连接等逻辑可扩展
            log.info("订单完成: orderId={}, userId={}", order.getId(), order.getUserId());
        }
    }

    // 根据时间段计算电价
    private double calcUnitPrice(LocalTime now, List<UnitPricePeriod> prices) {
        if (prices == null || prices.isEmpty()) return 1.0; // 可设默认值
        for (UnitPricePeriod p : prices) {
            if (isInPeriod(now, p.getStartTime(), p.getEndTime())) {
                return p.getPrice();
            }
        }
        return prices.get(0).getPrice(); // 默认用第一个时段
    }

    private boolean isInPeriod(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            // 跨夜
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}