package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.zhuzhu_charging_station_backend.dto.OrderUpsertRequest;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;
import org.zhuzhu_charging_station_backend.exception.NotFoundException;
import org.zhuzhu_charging_station_backend.exception.ForbiddenException;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final RedisTemplate<String, Order> orderRedisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final OrderRepository orderRepository;
    private final JwtTokenUtil jwtTokenUtil;

    private static final String FAST_QUEUE_KEY = "queue:fast";
    private static final String SLOW_QUEUE_KEY = "queue:slow";
    private static final String ORDER_KEY_PREFIX = "order:";

    /**
     * 新建或修改订单，自动分配（新）排队号并存入redis
     */
    public Order upsertOrder(OrderUpsertRequest req, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order;
        boolean isNew = (req.getId() == null);

        if (isNew) {
            long orderId = IdGenerator.generateUniqueOrderId(orderRepository, 10);
            order = new Order();
            order.setId(orderId);
            order.setUserId(userId);
        } else {
            order = getOrderFromRedis(req.getId());
            if (order == null) {
                throw new NotFoundException("订单不存在！");
            }
            if (!userId.equals(order.getUserId())) {
                throw new ForbiddenException("无权限操作他人订单！");
            }
            removeFromQueue(order);
        }
        order.setMode(req.getMode());
        order.setChargeAmount(req.getChargeAmount());
        order.setQueueNo(assignQueueNo(order.getMode()));
        String queueKey = order.getMode() == 1 ? FAST_QUEUE_KEY : SLOW_QUEUE_KEY;

        // 重新加入当前队列，存入Redis
        stringRedisTemplate.opsForSet().add(queueKey, String.valueOf(order.getId()));
        orderRedisTemplate.opsForValue().set(ORDER_KEY_PREFIX + order.getId(), order);
        return order;
    }

    /**
     * 取消订单（从队列和Redis移除，并入库标记为已取消）
     */
    public void cancelOrder(Long orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = getOrderFromRedis(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在！");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限取消他人订单！");
        }
        removeFromQueue(order);
        order.setStatus(-1); // -1:已取消
        orderRepository.save(order);
        orderRedisTemplate.delete(ORDER_KEY_PREFIX + orderId);
    }

    /**
     * 完结订单（从队列移除，更新订单状态和充电细节）
     */
    public void finishOrder(Long orderId, String token, FinishInfo finishInfo) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = getOrderFromRedis(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在！");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限操作他人订单！");
        }
        removeFromQueue(order);
        // 设置完结的业务字段
        order.setStatus(0); // 0:已完成
        order.setChargeAmount(finishInfo.chargeAmount);
        order.setChargeDuration(finishInfo.chargeDuration);
        order.setStartTime(finishInfo.startTime);
        order.setStopTime(finishInfo.stopTime);
        order.setChargeFee(finishInfo.chargeFee);
        order.setServiceFee(finishInfo.serviceFee);
        order.setTotalFee(finishInfo.totalFee);
        order.setChargingStationId(finishInfo.chargingStationId);
        orderRepository.save(order);
        orderRedisTemplate.delete(ORDER_KEY_PREFIX + orderId);
    }

    /**
     * 排队号分配：总是取当前队列最大+1
     */
    private String assignQueueNo(int mode) {
        String queueKey = mode == 1 ? FAST_QUEUE_KEY : SLOW_QUEUE_KEY;
        String prefix = mode == 1 ? "F" : "T";
        int maxNo = 0;
        Set<String> orderIds = stringRedisTemplate.opsForSet().members(queueKey);
        if (orderIds != null && !orderIds.isEmpty()) {
            for (String id : orderIds) {
                Order o = orderRedisTemplate.opsForValue().get(ORDER_KEY_PREFIX + id);
                if (o != null && o.getQueueNo() != null && o.getQueueNo().startsWith(prefix)) {
                    try {
                        int no = Integer.parseInt(o.getQueueNo().substring(1));
                        if (no > maxNo) maxNo = no;
                    } catch (NumberFormatException ignore) { }
                }
            }
        }
        return prefix + (maxNo + 1);
    }

    /**
     * 移除订单ID出队
     */
    private void removeFromQueue(Order order) {
        String queueKey = order.getMode() == 1 ? FAST_QUEUE_KEY : SLOW_QUEUE_KEY;
        stringRedisTemplate.opsForSet().remove(queueKey, String.valueOf(order.getId()));
    }

    /**
     * Redis取Order
     */
    private Order getOrderFromRedis(Long orderId) {
        return orderRedisTemplate.opsForValue().get(ORDER_KEY_PREFIX + orderId);
    }

    /**
     * 用于finishOrder参数传递
     */
    public static class FinishInfo {
        public Double chargeAmount;
        public Long chargeDuration;
        public LocalDateTime startTime;
        public LocalDateTime stopTime;
        public Double chargeFee;
        public Double serviceFee;
        public Double totalFee;
        public Long chargingStationId;
    }
}