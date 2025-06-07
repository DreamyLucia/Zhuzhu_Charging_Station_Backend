package org.zhuzhu_charging_station_backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zhuzhu_charging_station_backend.entity.*;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;
import org.zhuzhu_charging_station_backend.service.ChargingStationService;
import org.zhuzhu_charging_station_backend.service.ChargingStationSlotService;
import org.zhuzhu_charging_station_backend.service.OrderCacheService;
import org.zhuzhu_charging_station_backend.service.QueueService;
import org.zhuzhu_charging_station_backend.dto.ChargingStationResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChargingStationScheduler {

    private final ChargingStationService chargingStationService;
    private final OrderRepository orderRepository;
    private final ChargingStationSlotService chargingStationSlotService;
    private final QueueService queueService;
    private final OrderCacheService orderCacheService;

    @Scheduled(cron = "*/1 * * * * *") // 每秒执行一次
    public void chargingStationTask() {
        List<Long> ids = chargingStationService.getAllStationIds();
        for (Long id : ids) {
            try {
                chargingStationSlotService.updateSlotWithLock(id, slot -> {
                    if (slot == null) {
                        return;
                    }

                    if (slot.getStatus() == null
                            || slot.getStatus().getStatus() == 2
                            || slot.getStatus().getStatus() == 3) {
                        // 桩关闭或故障
                        return;
                    }
                    // Slot 业务逻辑只需要 id
                    processQueueFill(id, slot);
                    processChargingHeadOrder(id, slot);
                });
            } catch (Exception e) {
                log.error("调度充电桩[{}]异常", id, e);
            }
        }
    }

    // 补全队列
    private void processQueueFill(Long stationId, ChargingStationSlot slot) {
        ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(stationId);

        int needFill = station.getMaxQueueLength() - (slot.getQueue() == null ? 0 : slot.getQueue().size());
        if (needFill <= 0) return;
        // 使用QueueService获取队列所有订单ID
        Set<String> waitingOrderIds = queueService.getAllOrderIdsInQueue(station.getMode());
        if (waitingOrderIds == null || waitingOrderIds.isEmpty())
            return;
        if (slot.getQueue() == null)
            slot.setQueue(new ArrayList<>());

        for (String orderIdStr : waitingOrderIds) {
            if (slot.getQueue().size() >= station.getMaxQueueLength())
                break;
            Long orderId;
            try {
                orderId = Long.parseLong(orderIdStr);
            } catch (Exception ignore) {
                continue;
            }
            if (slot.getQueue().contains(orderId))
                continue;
            Order order = orderCacheService.getOrder(orderId);
            if (order == null)
                continue;
            if (order.getStatus() == 3) { // 3=等待中
                order.setStatus(2); // 2=排队中
                order.setQueueNo(null);
                orderCacheService.saveOrder(order); // 更新缓存
                queueService.removeOrderFromQueueWithLock(station.getMode(), orderIdStr);
                slot.getQueue().add(orderId);
            }
        }
    }

    // 处理队首订单
    private void processChargingHeadOrder(Long stationId, ChargingStationSlot slot) {
        ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(stationId);

        if (slot.getQueue() == null || slot.getQueue().isEmpty()) return;
        Long orderId = slot.getQueue().get(0);
        Order order = orderCacheService.getOrder(orderId);
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
        double addedCharge = station.getPower() / 3600.0; // 度/每秒
        order.setActualCharge(order.getActualCharge() + addedCharge);

        double unitPrice = calcUnitPrice(LocalTime.now(), station);
        order.setChargeFee(order.getChargeFee() + unitPrice * addedCharge);
        double addedService = station.getServiceFee() / 3600.0;
        order.setServiceFee(order.getServiceFee() + addedService);
        order.setTotalFee(order.getChargeFee() + order.getServiceFee());

        orderCacheService.saveOrder(order); // 更新缓存

        // 判断是否充满
        if (order.getActualCharge() >= order.getChargeAmount()) {
            LocalDateTime stop = LocalDateTime.now();
            order.setStopTime(stop);
            order.setStatus(0); // 已完成
            orderCacheService.deleteOrder(orderId); // 从缓存移除
            orderRepository.save(order); // 入库
            slot.getQueue().remove(0);
            // 发消息/推送等可扩展
            log.info("订单完成: orderId={}, userId={}", order.getId(), order.getUserId());
        }
    }

    // 根据时间段计算电价
    private double calcUnitPrice(LocalTime now, ChargingStationResponse station) {
        boolean isPeak = (now.compareTo(LocalTime.of(10, 0)) >= 0 && now.compareTo(LocalTime.of(15, 0)) < 0) ||
                (now.compareTo(LocalTime.of(18, 0)) >= 0 && now.compareTo(LocalTime.of(21, 0)) < 0);
        boolean isValley = (now.compareTo(LocalTime.of(23, 0)) >= 0 || now.compareTo(LocalTime.of(7, 0)) < 0);

        if (isPeak) {
            return station.getPeakPrice();
        } else if (isValley) {
            return station.getValleyPrice();
        } else {
            return station.getNormalPrice();
        }
    }
}