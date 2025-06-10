package org.zhuzhu_charging_station_backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderUpsertRequest {
    private String id;             // 详单ID，空则新增
    private Integer mode;        // 0:慢充，1:快充
    private BigDecimal chargeAmount;
}