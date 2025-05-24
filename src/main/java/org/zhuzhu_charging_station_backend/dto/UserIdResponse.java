package org.zhuzhu_charging_station_backend.dto;

public class UserIdResponse {
    private Long user_id;

    // 构造方法
    public UserIdResponse(Long userId) {
        this.user_id = userId;
    }

    // Getter
    public Long getUser_id() {
        return user_id;
    }
}