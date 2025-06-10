package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.zhuzhu_charging_station_backend.entity.Order;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String FAST_QUEUE_KEY = "queue:fast";
    private static final String SLOW_QUEUE_KEY = "queue:slow";

    @Autowired
    private RedisTemplate<String, String> longRedisTemplate;
    private final RedissonClient redissonClient;
    private final OrderCacheService orderCacheService;

    /**
     * 通过mode获取队列key
     * @param mode 1(快充)/其它(慢充)
     */
    public String getQueueKey(int mode) {
        return mode == 1 ? FAST_QUEUE_KEY : SLOW_QUEUE_KEY;
    }

    /**
     * 获取排队号（形如F1,RF3,T2,RT5），带分布式锁保证并发安全
     * 会从当前队列所有订单id中挑前缀相同的最大编号+1
     * @param mode 模式
     * @param prefix 排队号前缀，例: "RF"
     */
    public String assignQueueNoWithLock(int mode, String prefix) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            int maxNo = 0;
            List<String> orderIds = longRedisTemplate.opsForList().range(queueKey, 0, -1);
            if (orderIds != null && !orderIds.isEmpty()) {
                for (String id : orderIds) {
                    Order order = orderCacheService.getOrder(id);
                    if (order == null) continue;
                    String queueNo = order.getQueueNo();
                    if (queueNo != null && queueNo.startsWith(prefix)) {
                        try {
                            int no = Integer.parseInt(queueNo.substring(prefix.length()));
                            if (no > maxNo) {
                                maxNo = no;
                            }
                        } catch (NumberFormatException ignore) {}
                    }
                }
            }
            return prefix + (maxNo + 1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 入队（队尾），带分布式锁
     * @param mode 模式
     * @param orderId 订单ID
     */
    public void addOrderToQueueWithLock(int mode, String orderId) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            longRedisTemplate.opsForList().rightPush(queueKey, orderId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 队头插入，带分布式锁
     * @param mode 模式
     * @param orderId 订单ID
     */
    public void addOrderToQueueHeadWithLock(int mode, String orderId) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            longRedisTemplate.opsForList().leftPush(queueKey, orderId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 出队/移除，带分布式锁
     * @param mode 模式
     * @param orderId 订单ID
     */
    public void removeOrderFromQueueWithLock(int mode, String orderId) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            longRedisTemplate.opsForList().remove(queueKey, 0, orderId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查询队列所有订单ID
     */
    public List<String> getAllOrderIdsInQueue(int mode) {
        String queueKey = getQueueKey(mode);
        return longRedisTemplate.opsForList().range(queueKey, 0, -1);
    }

    /**
     * 释放一批订单到等待区队首（RF/RT编号，充电桩释放），带分布式锁
     * 这些订单会分配新的排队号(queueNo)和status、chargingStationId
     * @param orderIds 要释放的订单id列表
     * @param mode 1(快充)/其它(慢充)
     */
    public void releaseOrdersToQueueHead(List<String> orderIds, int mode) {
        if (orderIds == null || orderIds.isEmpty()) return;

        for (String orderId : orderIds) {
            Order order = orderCacheService.getOrder(orderId);
            if (order == null) continue;
            // 生成新的排队号
            String prefix = (mode == 1 ? "RF" : "RT");
            String newQueueNo = assignQueueNoWithLock(mode, prefix);
            order.setQueueNo(newQueueNo);
            order.setStatus(3); // 重新设置为“等待中”
            order.setChargingStationId(null); // 释放掉绑定
            orderCacheService.saveOrder(order);
            // 插入到队首
            addOrderToQueueHeadWithLock(mode, orderId);
        }
    }
}