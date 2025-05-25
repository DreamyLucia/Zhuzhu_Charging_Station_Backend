package org.zhuzhu_charging_station_backend.entity;

import javax.persistence.Embeddable;

// 累计充电报表信息，直接嵌入到 ChargingStation
@Embeddable
public class ReportInfo {

    private Integer totalChargeCount;    // 累计充电次数
    private Long totalChargeTime;        // 累计充电时长
    private Double totalChargeAmount;    // 累计充电量
    private Double totalChargeFee;       // 累计充电费用（元）
    private Double totalServiceFee;      // 累计服务费用（元）
    private Double totalFee;             // 累计总费用（元）

    public ReportInfo() {}

    // Getters and Setters

    public Integer getTotalChargeCount() {
        return totalChargeCount;
    }

    public void setTotalChargeCount(Integer totalChargeCount) {
        this.totalChargeCount = totalChargeCount;
    }

    public Long getTotalChargeTime() {
        return totalChargeTime;
    }

    public void setTotalChargeTime(Long totalChargeTime) {
        this.totalChargeTime = totalChargeTime;
    }

    public Double getTotalChargeAmount() {
        return totalChargeAmount;
    }

    public void setTotalChargeAmount(Double totalChargeAmount) {
        this.totalChargeAmount = totalChargeAmount;
    }

    public Double getTotalChargeFee() {
        return totalChargeFee;
    }

    public void setTotalChargeFee(Double totalChargeFee) {
        this.totalChargeFee = totalChargeFee;
    }

    public Double getTotalServiceFee() {
        return totalServiceFee;
    }

    public void setTotalServiceFee(Double totalServiceFee) {
        this.totalServiceFee = totalServiceFee;
    }

    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }
}