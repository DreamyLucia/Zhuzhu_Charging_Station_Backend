package org.zhuzhu_charging_station_backend.dto;

import lombok.Data;

@Data
public class OrderUpsertRequest {
    private Long id;             // 详单ID，空则新增
    private Integer mode;        // 0:慢充，1:快充
    private Double chargeAmount;
}