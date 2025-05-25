package org.zhuzhu_charging_station_backend.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 修改用户信息的请求体
 */
@Data
public class UpdateUserRequest {
    @NotBlank(message = "新用户名不能为空")
    private String newUsername;
}