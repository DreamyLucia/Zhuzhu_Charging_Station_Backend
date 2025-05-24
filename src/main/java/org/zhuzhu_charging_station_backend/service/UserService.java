package org.zhuzhu_charging_station_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.zhuzhu_charging_station_backend.dto.UserResponse;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;
import org.zhuzhu_charging_station_backend.util.PasswordUtil;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public UserService(UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Transactional
    public StandardResponse<UserResponse> register(String username, String password) {
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

            // 保存用户并获取完整的实体对象
            user = userRepository.saveAndFlush(user);

            // 4. 返回UserResponse
            logger.info("用户注册成功，ID: {}, Created At: {}, Updated At: {}", userId, user.getCreatedAt(), user.getUpdatedAt());
            return StandardResponse.success(new UserResponse(user));

        } catch (DataIntegrityViolationException e) {
            logger.error("数据冲突: {}", e.getMessage());
            return StandardResponse.error(500, "系统繁忙，请重试");
        } catch (IllegalStateException e) {
            logger.error("ID生成失败: {}", e.getMessage());
            return StandardResponse.error(500, "系统资源不足");
        }
    }

    @Transactional(readOnly = true)
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));

        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        logger.info("用户验证成功");

        return jwtTokenUtil.generateToken(user);
    }
}