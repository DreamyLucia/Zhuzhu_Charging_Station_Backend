package org.zhuzhu_charging_station_backend.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.zhuzhu_charging_station_backend.dto.RegisterRequest;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;
import org.zhuzhu_charging_station_backend.dto.UserIdResponse;
import org.zhuzhu_charging_station_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
                        "username": "testuser"
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
    public StandardResponse<UserIdResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return userService.register(request.getUsername(), request.getPassword());
    }
}