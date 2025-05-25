package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

import javax.persistence.Embeddable;

// 累计充电报表信息，直接嵌入到 ChargingStation
@Data
@Embeddable
public class ReportInfo {
    private Integer totalChargeCount;    // 累计充电次数
    private Long totalChargeTime;        // 累计充电时长
    private Double totalChargeAmount;    // 累计充电量
    private Double totalChargeFee;       // 累计充电费用（元）
    private Double totalServiceFee;      // 累计服务费用（元）
    private Double totalFee;             // 累计总费用（元）
}