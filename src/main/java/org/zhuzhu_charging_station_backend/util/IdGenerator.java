package org.zhuzhu_charging_station_backend.util;

import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class IdGenerator {
    private static final long MIN_ID = 10_000_000L; // 最小值：10000000
    private static final long MAX_ID = 99_999_999L; // 最大值：99999999
    private static final Random random = new SecureRandom();

    /**
     * 生成唯一的8位用户ID
     * @param userRepository 用于检查ID是否存在
     * @param maxAttempts 最大尝试次数（建议3-5次）
     */
    public static long generateUniqueUserId(UserRepository userRepository, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            long candidateId = MIN_ID + random.nextInt((int)(MAX_ID - MIN_ID + 1));
            if (!userRepository.existsById(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("无法生成唯一用户ID");
    }
}