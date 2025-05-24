package org.zhuzhu_charging_station_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.zhuzhu_charging_station_backend.dto.UserIdResponse;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.zhuzhu_charging_station_backend.util.PasswordUtil;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public StandardResponse<UserIdResponse> register(String username, String password) {
        try {
            // 1. 检查用户名唯一性
            if (userRepository.existsByUsername(username)) {
                logger.warn("用户名已存在: {}", username);
                return StandardResponse.error(400, "用户名已存在");
            }

            // 2. 生成唯一ID
            long userId = IdGenerator.generateUniqueUserId(userRepository, 10);
            logger.debug("生成用户ID: {}", userId);

            // 3. 创建并保存用户
            User user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setPassword(PasswordUtil.encode(password));
            userRepository.save(user);

            logger.info("用户注册成功，ID: {}", userId);
            return StandardResponse.success(new UserIdResponse(userId));

        } catch (DataIntegrityViolationException e) {
            logger.error("数据冲突（可能并发导致）: {}", e.getMessage());
            return StandardResponse.error(500, "系统繁忙，请重试");
        } catch (IllegalStateException e) {
            logger.error("ID生成失败: {}", e.getMessage());
            return StandardResponse.error(500, "系统资源不足");
        }
    }
}