package org.zhuzhu_charging_station_backend.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改密码请求体
 */
@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}