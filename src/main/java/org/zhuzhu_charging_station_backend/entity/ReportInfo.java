package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

import javax.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 累计充电报表信息，直接嵌入到 ChargingStation
@Data
@Embeddable
public class ReportInfo {
    private LocalDateTime updatedAt;      // 报表更新时间
    private Integer totalChargeCount;    // 累计充电次数
    private Long totalChargeTime;        // 累计充电时长
    private BigDecimal totalChargeAmount;    // 累计充电量
    private BigDecimal totalChargeFee;       // 累计充电费用（元）
    private BigDecimal totalServiceFee;      // 累计服务费用（元）
    private BigDecimal totalFee;             // 累计总费用（元）
}