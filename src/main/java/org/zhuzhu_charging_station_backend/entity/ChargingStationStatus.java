package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

// 充电桩实时状态信息（仅存于Redis）
@Data
public class ChargingStationStatus {
    private Integer status;  // 当前状态：0-正常，1-关闭，2-故障
    private Integer currentChargeCount;  // 系统启动后的累计充电次数
    private Long currentChargeTime;  // 系统启动后的累计充电总时长
    private Double currentChargeAmount;  // 系统启动后的累计充电量
}