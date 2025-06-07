package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String FAST_QUEUE_KEY = "queue:fast";
    private static final String SLOW_QUEUE_KEY = "queue:slow";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 通过mode获取队列key
     * @param mode 1(快充)/其它(慢充)
     */
    public String getQueueKey(int mode) {
        return mode == 1 ? FAST_QUEUE_KEY : SLOW_QUEUE_KEY;
    }

    /**
     * 获取排队号（形如F1,T2），带分布式锁保证并发安全
     */
    public String assignQueueNoWithLock(int mode, String prefix) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            int maxNo = 0;
            Set<String> orderIds = stringRedisTemplate.opsForSet().members(queueKey);
            if (orderIds != null && !orderIds.isEmpty()) {
                for (String id : orderIds) {
                    if (id.startsWith(prefix)) {
                        try {
                            int no = Integer.parseInt(id.substring(1));
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
     * 入队，带分布式锁
     */
    public void addOrderToQueueWithLock(int mode, String orderId) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            stringRedisTemplate.opsForSet().add(queueKey, orderId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 出队/移除，带分布式锁
     */
    public void removeOrderFromQueueWithLock(int mode, String orderId) {
        String queueKey = getQueueKey(mode);
        String lockKey = "lock:queue:" + queueKey;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            stringRedisTemplate.opsForSet().remove(queueKey, orderId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查询队列所有订单ID
     */
    public Set<String> getAllOrderIdsInQueue(int mode) {
        String queueKey = getQueueKey(mode);
        return stringRedisTemplate.opsForSet().members(queueKey);
    }
}