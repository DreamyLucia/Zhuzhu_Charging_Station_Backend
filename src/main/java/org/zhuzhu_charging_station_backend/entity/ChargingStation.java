package org.zhuzhu_charging_station_backend.entity;

import javax.persistence.*;

@Entity
public class ChargingStation {

    @Id
    private Long id; // 6位充电桩ID

    @Column(nullable = false, unique = true)
    private String name; // 充电桩名字

    @Column(nullable = false)
    private Integer mode; // 充电模式，0：慢充，1：快充

    @Column(nullable = false)
    private Double power; // 功率

    // 当前状态信息
    @Column(nullable = false)
    private Integer status; // 0 正常，1 关闭，2 故障

    @Column(nullable = false)
    private Integer currentChargeCount; // 系统启动后的累计充电次数

    @Column(nullable = false)
    private Long currentChargeTime; // 系统启动后的累计充电总时长

    @Column(nullable = false)
    private Double currentChargeAmount; // 系统启动后的累计充电总电量

    // 报表信息
    @Embedded
    private ReportInfo report;

    public ChargingStation() {}

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public Double getPower() {
        return power;
    }

    public void setPower(Double power) {
        this.power = power;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCurrentChargeCount() {
        return currentChargeCount;
    }

    public void setCurrentChargeCount(Integer currentChargeCount) {
        this.currentChargeCount = currentChargeCount;
    }

    public Long getCurrentChargeTime() {
        return currentChargeTime;
    }

    public void setCurrentChargeTime(Long currentChargeTime) {
        this.currentChargeTime = currentChargeTime;
    }

    public Double getCurrentChargeAmount() {
        return currentChargeAmount;
    }

    public void setCurrentChargeAmount(Double currentChargeAmount) {
        this.currentChargeAmount = currentChargeAmount;
    }

    public ReportInfo getReport() {
        return report;
    }

    public void setReport(ReportInfo report) {
        this.report = report;
    }
}