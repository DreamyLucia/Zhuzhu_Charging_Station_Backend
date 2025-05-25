package org.zhuzhu_charging_station_backend.controller;

import org.zhuzhu_charging_station_backend.dto.*;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public StandardResponse<LoginResponse> register(
            @Valid @RequestBody UserRequest request
    ) {
        // 注册成功后自动登录，返回token和用户信息
        return StandardResponse.success(userService.registerAndLogin(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public StandardResponse<LoginResponse> login(
            @Valid @RequestBody UserRequest request
    ) {
        return StandardResponse.success(userService.loginWithToken(request));
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public StandardResponse<UserResponse> getInfo() {
        // 直接取 userId
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return StandardResponse.success(new UserResponse(user));
    }

    @PutMapping("/username")
    @Operation(summary = "修改当前用户信息")
    public StandardResponse<UserResponse> updateUsername(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserResponse userResponse = userService.updateUsername(userId, request.getNewUsername());
        return StandardResponse.success(userResponse);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public StandardResponse<Void> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return StandardResponse.success();
    }
}