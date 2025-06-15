package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zhuzhu_charging_station_backend.dto.OrderUpsertRequest;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.entity.ChargingStationStatus;
import org.zhuzhu_charging_station_backend.exception.BadStateException;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;
import org.zhuzhu_charging_station_backend.exception.NotFoundException;
import org.zhuzhu_charging_station_backend.exception.ForbiddenException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final QueueService queueService;
    private final OrderCacheService orderCacheService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final ChargingStationService chargingStationService;
    private final ChargingStationSlotService chargingStationSlotService;

    /**
     * 新建或修改订单，自动分配（新）排队号并存入redis
     */
    public Order upsertOrder(OrderUpsertRequest req, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order;
        boolean isNew = (req.getId() == null);

        if (isNew) {
            String orderId = IdGenerator.generateUniqueOrderId(orderRepository, 10);
            order = new Order();
            order.setId(orderId);
            order.setUserId(userId);
            order.setRecordTime(LocalDateTime.now());
            order.setStatus(3);
        } else {
            order = orderCacheService.getOrder(req.getId());
            if (order == null) {
                throw new NotFoundException("订单不存在！");
            }
            if (!userId.equals(order.getUserId())) {
                throw new ForbiddenException("无权限操作他人订单！");
            }
            queueService.removeOrderFromQueueWithLock(order.getMode(), order.getId());
        }
        order.setMode(req.getMode());
        order.setChargeAmount(req.getChargeAmount());
        order.setQueueNo(queueService.assignQueueNoWithLock(order.getMode(), order.getMode() == 1 ? "F" : "T"));
        queueService.addOrderToQueueWithLock(order.getMode(), order.getId());
        orderCacheService.saveOrder(order);
        return order;
    }

    /**
     * 查询订单
     */
    public Order getOrder(String orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            // Cache未命中，去数据库查
            order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new NotFoundException("订单不存在！");
            }
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限查询他人订单！");
        }
        return order;
    }

    /**
     * 所有查询订单
     */
    public List<Order> getAllOrdersByUser(Long userId) {

        // 1. 数据库所有订单
        List<Order> dbOrders = orderRepository.findByUserId(userId);

        // 2. 缓存所有订单
        List<Order> cachedOrders = orderCacheService.getAllOrdersByUser(userId);

        // 3. 合并，去重（按order id去重，缓存优先生效）
        Map<String, Order> orderMap = new HashMap<>();
        for (Order o : dbOrders) {
            orderMap.put(o.getId(), o);
        }
        for (Order o : cachedOrders) {
            orderMap.put(o.getId(), o); // cache覆盖db
        }
        // 如果你想cache中的覆盖db中的
        return new ArrayList<>(orderMap.values());
    }

    /**
     * 取消订单
     */
    public Order cancelOrder(String orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在！");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限取消他人订单！");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new BadStateException("订单当前状态不可取消！");
        }
        if (order.getActualCharge() != null && order.getActualCharge().compareTo(BigDecimal.ZERO) >= 0) {
            return settleOrder(orderId);
        } else {
            // 1. 移除分配前的队列
            queueService.removeOrderFromQueueWithLock(order.getMode(), order.getId());

            // 2. 移除slot队列
            if (order.getChargingStationId() != null) {
                Long stationId = order.getChargingStationId();
                chargingStationSlotService.updateSlotWithLock(stationId, slot -> {
                    if (slot != null && slot.getQueue() != null) {
                        slot.getQueue().remove(orderId);
                    }
                });
            }

            // 3. 修改/删除本地/缓存订单
            order.setStatus(4); // 4: 已取消
            orderRepository.save(order);
            orderCacheService.deleteOrder(orderId);

            return order;
        }
    }

    /**
     * 完结订单
     */
    public Order finishOrder(String orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在！");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限操作他人订单！");
        }
        if (order.getStatus() != 1) {
            throw new BadStateException("订单当前状态不可完结！");
        }
        return settleOrder(orderId);
    }

    /**
     * 完结订单，移除所有相关队列，更新slot及报表，同步缓存和数据库。
     */
    public Order settleOrder(String orderId) {
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) throw new NotFoundException("订单不存在");

        // 1. 移除全局排队队列
        queueService.removeOrderFromQueueWithLock(order.getMode(), orderId);

        // 2. slot更新
        Long stationId = order.getChargingStationId();
        chargingStationSlotService.updateSlotWithLock(stationId, slot -> {
            if (slot == null) return;

            if (slot.getQueue() != null) {
                slot.getQueue().remove(orderId);
            }

            ChargingStationStatus status = slot.getStatus();
            if (status != null) {
                status.setStatus(0); // 0-空闲中
                status.setCurrentChargeCount((status.getCurrentChargeCount() == null ? 0 : status.getCurrentChargeCount()) + 1);
                status.setCurrentChargeTime((status.getCurrentChargeTime() == null ? 0L : status.getCurrentChargeTime()) + order.getChargeDuration());
                status.setCurrentChargeAmount((status.getCurrentChargeAmount() == null ? 0.0 : status.getCurrentChargeAmount()) +
                        (order.getActualCharge() != null ? order.getActualCharge().doubleValue() : 0.0));
                slot.setStatus(status);
            }
        });

        // 3. 订单状态及时间
        order.setStatus(0); // 0:已完成
        order.setStopTime(LocalDateTime.now());

        // 4. 用户累计信息更新
        User user = userRepository.findById(order.getUserId()).orElseThrow(() -> new NotFoundException("用户不存在"));
        user.setTotalChargeCount(user.getTotalChargeCount() + 1);
        user.setTotalChargeAmount(user.getTotalChargeAmount().add(order.getActualCharge()));
        user.setTotalChargeDuration(user.getTotalChargeDuration() + order.getChargeDuration());
        user.setTotalChargeFee(user.getTotalChargeFee().add(order.getChargeFee()));
        user.setTotalServiceFee(user.getTotalServiceFee().add(order.getServiceFee()));
        user.setTotalFee(user.getTotalFee().add(order.getTotalFee()));
        userRepository.save(user);

        // 5. 缓存同步&入库
        orderCacheService.deleteOrder(orderId);
        orderRepository.save(order);

        // 6. 充电桩报表更新
        chargingStationService.updateReportInfo(stationId, order);

        // 7. 发消息/推送
        log.info("订单完成: orderId={}, userId={}", order.getId(), order.getUserId());

        return order;
    }
}