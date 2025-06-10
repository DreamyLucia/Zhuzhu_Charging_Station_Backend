package org.zhuzhu_charging_station_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.zhuzhu_charging_station_backend.dto.UserRequest;
import org.zhuzhu_charging_station_backend.dto.UserResponse;
import org.zhuzhu_charging_station_backend.dto.LoginResponse;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.exception.AlreadyExistsException;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;
import org.zhuzhu_charging_station_backend.util.PasswordUtil;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    @Autowired
    private EntityManager entityManager;

    public UserService(UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 用户注册（注册成功后自动登录，返回token和用户信息）
     * @param request 用户请求体，包含用户名与密码
     * @return 登录响应体（含token及用户信息）
     */
    @Transactional
    public LoginResponse registerAndLogin(UserRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        try {
            // 1. 检查用户名唯一性
            if (userRepository.existsByUsername(username)) {
                logger.warn("用户名已存在: {}", username);
                throw new AlreadyExistsException("用户名已存在");
            }

            // 2. 生成唯一ID
            long userId = IdGenerator.generateUniqueUserId(userRepository, 10);
            logger.debug("生成用户ID: {}", userId);

            // 3. 创建并保存用户
            User user = new User();
            user.setUserId(userId);
            user.setUsername(username);
            user.setRoles("ROLE_USER"); // 默认角色
            user.setPassword(PasswordUtil.encode(password));

            // 保存用户并刷新会话
            user = userRepository.saveAndFlush(user);

            // 强制从数据库中重新加载实体对象
            entityManager.refresh(user);

            // 自动生成token
            String token = jwtTokenUtil.generateToken(user);

            // 4. 返回LoginResponse（含token和用户信息）
            logger.info("用户注册并自动登录成功，ID: {}, Created At: {}, Updated At: {}", userId, user.getCreatedAt(), user.getUpdatedAt());
            return new LoginResponse(token, new UserResponse(user));

        } catch (DataIntegrityViolationException e) {
            logger.error("数据冲突: {}", e.getMessage());
            throw new AlreadyExistsException("用户名已存在");
        } catch (IllegalStateException e) {
            logger.error("ID生成失败: {}", e.getMessage());
            throw new RuntimeException("系统资源不足");
        }
    }

    /**
     * 用户登录
     * @param request 用户请求体，包含用户名与密码
     * @return 登录响应体（含token及用户信息）
     */
    @Transactional(readOnly = true)
    public LoginResponse loginWithToken(UserRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));

        // 校验密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        logger.info("用户验证成功");

        // 生成token
        String token = jwtTokenUtil.generateToken(user);

        return new LoginResponse(token, new UserResponse(user));
    }

    /**
     * 重置用户密码
     * @param request 用户请求体，包含用户名与新的密码
     */
    @Transactional
    public void resetPassword(UserRequest request) {
        String username = request.getUsername();
        String newPassword = request.getPassword();

        // 查找用户，不存在则抛出异常
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 设置新密码（加密存储）
        user.setPassword(PasswordUtil.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 修改当前用户信息
     * @param userId 用户ID
     * @param newUsername 新的用户名
     * @return 用户响应体
     */
    @Transactional
    public UserResponse updateUsername(Long userId, String newUsername) {
        if (userRepository.existsByUsername(newUsername)) {
            throw new AlreadyExistsException("用户名已存在");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        user.setUsername(newUsername);
        userRepository.save(user);
        return new UserResponse(user);
    }

    /**
     * 修改当前用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("旧密码错误");
        }
        user.setPassword(PasswordUtil.encode(newPassword));
        userRepository.save(user);
    }
}