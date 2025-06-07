package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ChargingStationSlotService {

    private final RedisTemplate<String, ChargingStationSlot> slotRedisTemplate;
    private final RedissonClient redissonClient;

    private static final String SLOT_KEY_PREFIX = "slot:";
    private static final String SLOT_LOCK_PREFIX = "slot-lock:"; // 分布式锁前缀

    public ChargingStationSlot getSlot(Long stationId) {
        return slotRedisTemplate.opsForValue().get(SLOT_KEY_PREFIX + stationId);
    }

    public void setSlot(Long stationId, ChargingStationSlot slot) {
        slotRedisTemplate.opsForValue().set(SLOT_KEY_PREFIX + stationId, slot);
    }

    public void removeSlot(Long stationId) {
        slotRedisTemplate.delete(SLOT_KEY_PREFIX + stationId);
    }

    /**
     * 原子地更新充电桩slot状态，确保并发安全
     * @param stationId 桩ID
     * @param updater   对slot的操作
     */
    public void updateSlotWithLock(Long stationId, Consumer<ChargingStationSlot> updater) {
        String lockKey = SLOT_LOCK_PREFIX + stationId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(10, 10, TimeUnit.SECONDS);
            if (locked) {
                ChargingStationSlot slot = getSlot(stationId);
                if (slot != null) {
                    updater.accept(slot);
                    setSlot(stationId, slot);
                }
            } else {
                throw new RuntimeException("无法获得充电桩slot的更新锁：" + stationId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("尝试加锁被中断:" + stationId, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}