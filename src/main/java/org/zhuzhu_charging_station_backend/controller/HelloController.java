package org.zhuzhu_charging_station_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hello")
@Tag(name = "问候API", description = "简单的问候接口")
public class HelloController {

    @GetMapping
    @Operation(
            summary = "获取问候语",
            description = "返回一个简单的Hello World问候",
            responses = {
                    @ApiResponse(responseCode = "200", description = "成功响应"),
                    @ApiResponse(responseCode = "500", description = "服务器错误")
            }
    )
    public String sayHello() {
        return "Hello World";
    }

    @GetMapping("/{name}")
    @Operation(summary = "个性化问候")
    public String sayHelloToName(
            @Parameter(description = "用户姓名", example = "张三")
            @PathVariable String name) {
        return "Hello " + name;
    }
}