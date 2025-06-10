package org.zhuzhu_charging_station_backend.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zhuzhu_charging_station_backend.dto.ChargingStationResponse;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.service.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final ChargingStationService chargingStationService;
    private final ChargingStationSlotService chargingStationSlotService;
    private final QueueService queueService;
    private final OrderCacheService orderCacheService;

    // 支持快充（mode=1）和慢充（mode=0），可按需扩展
    @Scheduled(cron = "*/1 * * * * *")
    public void assignOrdersToSlot() {
        for (int mode : new int[]{1, 0}) {
            List<String> pendingOrderIds = queueService.getAllOrderIdsInQueue(mode);
            if (pendingOrderIds == null || pendingOrderIds.isEmpty()) {
                continue;
            }

            List<Long> stationIds = chargingStationService.getAllStationIds();

            for (String orderId : pendingOrderIds) {
                Order order = orderCacheService.getOrder(orderId);
                if (order == null) {
                    log.warn("订单 {} 不存在，已跳过", orderId);
                    continue;
                }

                Long bestStationId = null;
                Long minWaitingTime = null;

                for (Long stationId : stationIds) {
                    ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(stationId);
                    int stationMode = station.getMode();
                    if (stationMode != mode) continue; // mode不匹配直接跳过
                    ChargingStationSlot slot = chargingStationSlotService.getSlot(stationId);
                    if (slot == null || slot.getStatus() == null) continue;
                    int statusCode = slot.getStatus().getStatus();
                    if (statusCode == 2 || statusCode == 3) continue; // 跳过故障、关闭
                    List<String> slotQueue = slot.getQueue();
                    if (slotQueue == null) continue;
                    Integer maxQueueLength = chargingStationService.getChargingStationWithSlot(stationId).getMaxQueueLength();
                    if (maxQueueLength != null && slotQueue.size() >= maxQueueLength) continue;
                    long waitingTime = slot.getWaitingTime() != null ? slot.getWaitingTime() : 0L;
                    if (minWaitingTime == null || waitingTime < minWaitingTime) {
                        minWaitingTime = waitingTime;
                        bestStationId = stationId;
                    }
                }

                if (bestStationId != null) {
                    final Long targetId = bestStationId; // 保证final化
                    // 并发安全添加到slot队列
                    chargingStationSlotService.updateSlotWithLock(targetId, slot -> {
                        if (!slot.getQueue().contains(orderId)) {
                            slot.getQueue().add(orderId);
                            // 分配后，立即刷新 waitingTime
                            ChargingStationResponse station = chargingStationService.getChargingStationWithSlot(targetId);
                            slot.setWaitingTime(calcWaitingTime(slot, station));
                        }
                    });

                    // 更新订单分配结果
                    order.setStatus(2);
                    order.setChargingStationId(bestStationId);
                    orderCacheService.saveOrder(order);

                    // 从等待队列移除
                    queueService.removeOrderFromQueueWithLock(mode, orderId);

                    log.info("订单 {} 已分配至充电桩 {}", orderId, bestStationId);
                } else {
                    log.info("订单 {} 未找到可分配充电桩", orderId);
                }
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