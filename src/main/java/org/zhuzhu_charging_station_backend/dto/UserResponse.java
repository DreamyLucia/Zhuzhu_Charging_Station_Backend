package org.zhuzhu_charging_station_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.zhuzhu_charging_station_backend.entity.User;

import java.math.BigDecimal;
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

    @Schema(description = "累计充电次数", example = "100")
    private Integer totalChargeCount;

    @Schema(description = "累计充电电量", example = "1500.5")
    private BigDecimal totalChargeAmount;

    @Schema(description = "累计充电时长（秒）", example = "36000")
    private Long totalChargeDuration;

    @Schema(description = "累计充电费用", example = "1200.00")
    private BigDecimal totalChargeFee;

    @Schema(description = "累计服务费用", example = "200.00")
    private BigDecimal totalServiceFee;

    @Schema(description = "累计总费用", example = "1400.00")
    private BigDecimal totalFee;

    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.roles = Arrays.asList(user.getRoles().split(",")); // 将逗号分隔的字符串转为List
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.totalChargeCount = user.getTotalChargeCount();
        this.totalChargeAmount = user.getTotalChargeAmount();
        this.totalChargeDuration = user.getTotalChargeDuration();
        this.totalChargeFee = user.getTotalChargeFee();
        this.totalServiceFee = user.getTotalServiceFee();
        this.totalFee = user.getTotalFee();
    }
}