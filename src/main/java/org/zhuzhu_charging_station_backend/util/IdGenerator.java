package org.zhuzhu_charging_station_backend.util;

import org.zhuzhu_charging_station_backend.repository.ChargingStationRepository;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class IdGenerator {
    private static final int MIN_ID_SIX = 100_000;  // 6位最小
    private static final int MAX_ID_SIX = 999_999;  // 6位最大
    private static final long MIN_ID_EIGHT = 10_000_000L; // 8位最小
    private static final long MAX_ID_EIGHT = 99_999_999L; // 8位最大
    private static final long MIN_ID_SIXTEEN = 1_0000_0000_0000_0000L;   // 16位最小
    private static final long MAX_ID_SIXTEEN = 9_9999_9999_9999_9999L;   // 16位最大
    private static final Random random = new SecureRandom();

    /**
     * 生成唯一的8位用户ID
     * @param userRepository 用于检查ID是否存在
     * @param maxAttempts 最大尝试次数（建议3-5次）
     */
    public static long generateUniqueUserId(UserRepository userRepository, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            long candidateId = MIN_ID_EIGHT + random.nextInt((int)(MAX_ID_EIGHT - MIN_ID_EIGHT + 1));
            if (!userRepository.existsById(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("无法生成唯一用户ID");
    }

    /**
     * 生成唯一的6位充电桩ID
     * @param chargingStationRepository 检查ID是否存在
     * @param maxAttempts 最大尝试次数
     * @return 唯一6位充电桩ID
     */
    public static long generateUniqueStationId(ChargingStationRepository chargingStationRepository, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            long candidateId = MIN_ID_SIX + random.nextInt(MAX_ID_SIX - MIN_ID_SIX + 1);
            if (!chargingStationRepository.existsById(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("无法生成唯一充电桩ID");
    }

    /**
     * 生成唯一的16位详单ID
     * @param orderRepository 用于检查ID是否存在
     * @param maxAttempts 最大尝试次数
     * @return 唯一16位详单ID
     */
    public static long generateUniqueOrderId(OrderRepository orderRepository, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            long candidateId = MIN_ID_SIXTEEN + (long)(random.nextDouble() * (MAX_ID_SIXTEEN - MIN_ID_SIXTEEN + 1));
            if (!orderRepository.existsById(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("无法生成唯一详单ID");
    }
}