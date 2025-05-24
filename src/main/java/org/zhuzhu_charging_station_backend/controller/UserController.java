package org.zhuzhu_charging_station_backend.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.zhuzhu_charging_station_backend.dto.*;
import org.zhuzhu_charging_station_backend.entity.User;
import org.zhuzhu_charging_station_backend.repository.UserRepository;
import org.zhuzhu_charging_station_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public UserController(UserService userService, UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "注册成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardResponse.class),
                            examples = @ExampleObject(
                                    value = """
                {
                  "code": 200,
                  "msg": "Success",
                  "data": {
                    "userId": 1,
                    "username": "testuser",
                    "createdAt": "2023-01-01T00:00:00",
                    "updatedAt": "2023-01-01T00:00:00"
                  }
                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "用户名已存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardResponse.class),
                            examples = @ExampleObject(
                                    value = """
                {
                  "code": 400,
                  "msg": "用户名已存在",
                  "data": null
                }"""
                            )
                    )
            )
    })
    public StandardResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return userService.register(request.getUsername(), request.getPassword());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "登录成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardResponse.class),
                            examples = @ExampleObject(
                                    value = """
                {
                  "code": 200,
                  "msg": "Success",
                  "data": {
                    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    "user": {
                      "userId": 1,
                      "username": "testuser",
                      "createdAt": "2023-01-01T00:00:00",
                      "updatedAt": "2023-01-01T00:00:00"
                    }
                  }
                }"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "用户名或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardResponse.class),
                            examples = @ExampleObject(
                                    value = """
                {
                  "code": 401,
                  "msg": "用户名或密码错误",
                  "data": null
                }"""
                            )
                    )
            )
    })
    public StandardResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return StandardResponse.success(
                new LoginResponse(token, new UserResponse(user))
        );
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "成功返回用户信息",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                      "code": 200,
                      "msg": "Success",
                      "data": {
                        "userId": 1,
                        "username": "testuser",
                        "roles": ["ROLE_USER"],
                        "createdAt": "2023-01-01T00:00:00",
                        "updatedAt": "2023-01-01T00:00:00"
                      }
                    }"""
                            )
                    )
            )
    })
    public StandardResponse<UserResponse> getInfo() {
        // 直接取 userId
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return StandardResponse.success(new UserResponse(user));
    }
}