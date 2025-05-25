package org.zhuzhu_charging_station_backend.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Order {
    @Id
    private Long id; // 16位详单ID

    @Column(nullable = false)
    private Long userId; // 用户ID

    @Column(nullable = false)
    private Long chargingStationId; // 充电桩ID

    @Column(nullable = false)
    private Integer mode; // 充电模式，0:慢充，1:快充

    @Column(nullable = false)
    private LocalDateTime recordTime; // 详单生成时间

    @Column(nullable = false)
    private Integer status; // 订单状态，0:已完成，1:进行中

    @Column(nullable = false)
    private Double chargeAmount; // 充电电量

    @Column(nullable = false)
    private Long chargeDuration; // 充电时长

    @Column(nullable = false)
    private LocalDateTime startTime; // 启动时间

    @Column(nullable = false)
    private LocalDateTime stopTime; // 停止时间

    @Column(nullable = false)
    private Double chargeFee; // 充电费用

    @Column(nullable = false)
    private Double serviceFee; // 服务费用

    @Column(nullable = false)
    private Double totalFee; // 总费用

    public Order() {}

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getChargingStationId() { return chargingStationId; }
    public void setChargingStationId(Long chargingStationId) { this.chargingStationId = chargingStationId; }

    public Integer getMode() { return mode; }
    public void setMode(Integer mode) { this.mode = mode; }

    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Double getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(Double chargeAmount) { this.chargeAmount = chargeAmount; }

    public Long getChargeDuration() { return chargeDuration; }
    public void setChargeDuration(Long chargeDuration) { this.chargeDuration = chargeDuration; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getStopTime() { return stopTime; }
    public void setStopTime(LocalDateTime stopTime) { this.stopTime = stopTime; }

    public Double getChargeFee() { return chargeFee; }
    public void setChargeFee(Double chargeFee) { this.chargeFee = chargeFee; }

    public Double getServiceFee() { return serviceFee; }
    public void setServiceFee(Double serviceFee) { this.serviceFee = serviceFee; }

    public Double getTotalFee() { return totalFee; }
    public void setTotalFee(Double totalFee) { this.totalFee = totalFee; }
}