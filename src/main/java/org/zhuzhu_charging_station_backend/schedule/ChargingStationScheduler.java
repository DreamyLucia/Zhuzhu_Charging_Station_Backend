package org.zhuzhu_charging_station_backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zhuzhu_charging_station_backend.entity.*;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;
import org.zhuzhu_charging_station_backend.service.*;
import org.zhuzhu_charging_station_backend.dto.ChargingStationResponse;
import org.zhuzhu_charging_station_backend.util.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChargingStationScheduler {
    private final ChargingStationService chargingStationService;
    private final ChargingStationSlotService chargingStationSlotService;
    private final QueueService queueService;
    private final OrderCacheService orderCacheService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(cron = "*/1 * * * * *") // 每秒执行一次
    public void chargingStationTask() {
        List<Long> ids = chargingStationService.getAllStationIds();

        // 第一轮：全量刷新waitingTime
        for (Long id : ids) {
            try {
                chargingStationSlotService.updateSlotWithLock(id, slot -> {
                    if (slot == null) {
                        return;
                    }
                    ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(id);
                    if (slot.getStatus() == null
                            || slot.getStatus().getStatus() == 2
                            || slot.getStatus().getStatus() == 3) {
                        // 桩关闭或故障
                        releaseOrdersFromSlot(slot, station.getMode());
                        return;
                    }
                    long waitingTime = calcWaitingTime(slot, station);
                    slot.setWaitingTime(waitingTime);
                    // 状态自动刷新
                    if (slot.getQueue() == null || slot.getQueue().isEmpty()) {
                        if (slot.getStatus() != null) slot.getStatus().setStatus(0); // 空闲
                    } else {
                        if (slot.getStatus() != null) slot.getStatus().setStatus(1); // 使用中
                    }
                });
            } catch (Exception e) {
                log.error("刷新waitingTime失败: stationId={}", id, e);
            }
        }
        // 第二轮：推进业务
        for (Long id : ids) {
            try {
                chargingStationSlotService.updateSlotWithLock(id, slot -> {
                    if (slot == null) return;
                    ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(id);
                    if (slot.getStatus() == null
                            || slot.getStatus().getStatus() == 2
                            || slot.getStatus().getStatus() == 3) {
                        // 桩关闭或故障
                        releaseOrdersFromSlot(slot, station.getMode());
                        return;
                    }
                    processChargingHeadOrder(id, slot);
                });
            } catch (Exception e) {
                log.error("推进业务异常: stationId={}", id, e);
            }
        }
    }

    // 处理队首订单
    private void processChargingHeadOrder(Long stationId, ChargingStationSlot slot) {
        ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(stationId);
        if (slot.getQueue() == null || slot.getQueue().isEmpty()) return;
        String orderId = slot.getQueue().get(0);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            slot.getQueue().remove(0); // 移除无效单
            return;
        }
        if (order.getStatus() != 1) { // 非进行中，初始化
            order.setQueueNo("");
            order.setStatus(1);
            order.setActualCharge(BigDecimal.valueOf(0.0));
            order.setChargeDuration(0L);
            order.setChargeFee(BigDecimal.valueOf(0.0));
            order.setServiceFee(BigDecimal.valueOf(0.0));
            order.setTotalFee(BigDecimal.valueOf(0.0));
            order.setStartTime(LocalDateTime.now());
        }
        // 每秒推进
        order.setChargeDuration(order.getChargeDuration() + 1);
        BigDecimal addedCharge = station.getPower();
        order.setActualCharge(order.getActualCharge().add(addedCharge));

        BigDecimal unitPrice = calcUnitPrice(LocalTime.now(), station);
        order.setChargeFee(order.getActualCharge().multiply(unitPrice));
        order.setServiceFee(order.getActualCharge().multiply(station.getServiceFee()));
        order.setTotalFee(order.getChargeFee().add(order.getServiceFee()));

        // 实时写回Redis
        orderCacheService.saveOrder(order);

        // 判断是否充满
        if (order.getActualCharge().compareTo(order.getChargeAmount()) >= 0) {
            orderService.settleOrder(orderId);
        }
    }

    // 根据时间段计算电价
    private BigDecimal calcUnitPrice(LocalTime now, ChargingStationResponse station) {
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

    // 释放订单到等待区
    private void releaseOrdersFromSlot(ChargingStationSlot slot, int mode) {
        if (slot.getQueue() == null || slot.getQueue().isEmpty()) return;
        // 收集所有队列订单ID
        List<String> orderIds = new ArrayList<>(slot.getQueue());
        // 清空slot队列
        slot.getQueue().clear();

        if (orderIds.isEmpty()) return;

        // 处理队首订单
        String headOrderId = orderIds.get(0);
        orderIds.remove(0);
        queueService.releaseOrdersToQueueHead(orderIds, mode);
        Order headOrder = orderCacheService.getOrder(headOrderId);
        if (headOrder != null) {
            // 先结算当前订单（生成详单并关闭订单）
            orderService.settleOrder(headOrderId);

            // 计算剩余充电量
            BigDecimal actualCharge = headOrder.getActualCharge() == null ? BigDecimal.ZERO : headOrder.getActualCharge();
            BigDecimal remainingCharge = headOrder.getChargeAmount().subtract(actualCharge);

            if (remainingCharge.compareTo(BigDecimal.ZERO) > 0) {
                // 创建新订单对象，复制必要字段
                Order newOrder = new Order();
                newOrder.setId(IdGenerator.generateUniqueOrderId(orderRepository, 10));
                newOrder.setUserId(headOrder.getUserId());
                newOrder.setChargingStationId(headOrder.getChargingStationId());
                newOrder.setMode(headOrder.getMode());
                newOrder.setRecordTime(LocalDateTime.now());
                newOrder.setChargeAmount(remainingCharge);
                newOrder.setActualCharge(BigDecimal.ZERO);
                newOrder.setChargeDuration(0L);
                newOrder.setStartTime(null);
                newOrder.setStopTime(null);
                newOrder.setChargeFee(BigDecimal.ZERO);
                newOrder.setServiceFee(BigDecimal.ZERO);
                newOrder.setTotalFee(BigDecimal.ZERO);
                newOrder.setStatus(2); // 2: 排队中

                // 保存新订单（你可能需要调用订单服务的方法，我假设是 saveOrder）
                orderCacheService.saveOrder(newOrder);

                // 放回等待队列
                queueService.releaseOrdersToQueueHead(Collections.singletonList(newOrder.getId()), mode);
            }
        }
    }

    // 实时计算充电桩slot到本轮全部订单完成的等待总时长
    private long calcWaitingTime(ChargingStationSlot slot, ChargingStationResponse station) {
        if (slot == null || slot.getQueue() == null || slot.getQueue().isEmpty()) {
            return 0L;
        }
        long totalSeconds = 0L;
        for (String orderId : slot.getQueue()) {
            Order order = orderCacheService.getOrder(orderId);
            if (order == null) continue;
            BigDecimal remaining = order.getChargeAmount().subtract(
                    order.getActualCharge() == null ? BigDecimal.ZERO : order.getActualCharge()
            );
            // 防止负数或已完成
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
            long thisOrderSeconds = remaining
                    .divide(station.getPower(), 0, BigDecimal.ROUND_UP)
                    .longValue();
            totalSeconds += thisOrderSeconds;
        }
        return totalSeconds;
    }
}