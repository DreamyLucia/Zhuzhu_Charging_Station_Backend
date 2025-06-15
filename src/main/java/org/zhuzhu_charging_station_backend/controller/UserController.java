package org.zhuzhu_charging_station_backend.controller;

import org.zhuzhu_charging_station_backend.dto.*;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.service.OrderService;
import org.zhuzhu_charging_station_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "用户管理")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final JwtTokenUtil jwtTokenUtil;

    public UserController(UserService userService, UserRepository userRepository, OrderService orderService, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.jwtTokenUtil = jwtTokenUtil;
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

    @PostMapping("/reset")
    @Operation(summary = "重置密码")
    public StandardResponse<Void> reset(
            @Valid @RequestBody UserRequest request
    ) {
        userService.resetPassword(request);
        return StandardResponse.success();
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

    @GetMapping("/orders")
    @Operation(summary = "获取当前用户的所有订单")
    public StandardResponse<List<Order>> getUserOrders(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        List<String> roles = jwtTokenUtil.extractRoles(token);
        List<Order> orders;
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            orders = orderService.getAllOrders(); // 管理员查看所有订单
        } else {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            orders = orderService.getAllOrdersByUser(userId); // 普通用户只看自己订单
        }
        return StandardResponse.success(orders);
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