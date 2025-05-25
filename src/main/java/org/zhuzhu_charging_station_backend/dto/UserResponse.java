package org.zhuzhu_charging_station_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.zhuzhu_charging_station_backend.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@Data
public class UserResponse {
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "testuser")
    private String username;

    @Schema(description = "角色列表", example = "[\"ROLE_USER\"]")
    private List<String> roles;

    @Schema(description = "创建时间", example = "2023-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2023-01-01T00:00:00")
    private LocalDateTime updatedAt;

    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.roles = Arrays.asList(user.getRoles().split(",")); // 将逗号分隔的字符串转为List
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}