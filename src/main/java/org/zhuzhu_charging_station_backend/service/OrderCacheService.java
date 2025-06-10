package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.zhuzhu_charging_station_backend.entity.Order;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderCacheService {

    private static final String ORDER_KEY_PREFIX = "order:";

    private final RedisTemplate<String, Order> orderRedisTemplate;

    /**
     * 存储订单到Redis
     */
    public void saveOrder(Order order) {
        if (order == null || order.getId() == null) return;
        orderRedisTemplate.opsForValue().set(buildOrderKey(order.getId()), order);
    }

    /**
     * 从Redis获取订单
     */
    public Order getOrder(String orderId) {
        if (orderId == null) return null;
        return orderRedisTemplate.opsForValue().get(buildOrderKey(orderId));
    }

    public List<Order> getAllOrdersByUser(Long userId) {
        Set<String> keys = orderRedisTemplate.keys("order:*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();

        List<Order> result = new ArrayList<>();
        for (String key : keys) {
            Order order = orderRedisTemplate.opsForValue().get(key);
            if (order != null && Objects.equals(order.getUserId(), userId)) {
                result.add(order);
            }
        }
        return result;
    }

    /**
     * 从Redis删除订单
     */
    public void deleteOrder(String orderId) {
        if (orderId == null) return;
        orderRedisTemplate.delete(buildOrderKey(orderId));
    }

    /**
     * 构建Redis订单Key
     */
    public String buildOrderKey(String orderId) {
        return ORDER_KEY_PREFIX + orderId;
    }
}