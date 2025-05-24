package org.zhuzhu_charging_station_backend.controller;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.zhuzhu_charging_station_backend.dto.*;

@RestController
@RequestMapping("/api/hello")
@Tag(name = "问候API")
public class HelloController {

    @GetMapping
    @Operation(
            summary = "获取问候语",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "成功响应",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardResponse.class),
                                    examples = @ExampleObject(
                                            value = "{ \"code\": 200, \"msg\": \"Success\", \"data\": \"Hello World\" }"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "服务器错误",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardResponse.class),
                                    examples = @ExampleObject(
                                            value = "{ \"code\": 500, \"msg\": \"Service Unavailable\", \"data\": null }"
                                    )
                            )
                    )
            }
    )
    public StandardResponse<String> sayHello() {
        try {
            return StandardResponse.success("Hello World");
        } catch (Exception e) {
            return StandardResponse.error(500, "Service Unavailable");
        }
    }
}