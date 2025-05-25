package org.zhuzhu_charging_station_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargingStationUpsertRequest {
    private Long id;          // ID，null 表示创建，否则为修改
    private String name;      // 名字
    private Integer mode;     // 充电模式
    private Double power;     // 功率
    private Integer status;   // 充电桩状态
}