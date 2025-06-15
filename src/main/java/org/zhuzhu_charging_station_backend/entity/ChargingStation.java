package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
public class ChargingStation {

    @Id
    private Long id; // 6位充电桩ID

    @Column(nullable = false, unique = true)
    private String name; // 充电桩名字

    @Column(columnDefinition = "TEXT")
    private String description; // 充电桩描述

    @Column(nullable = false)
    private Integer mode; // 充电模式，0：慢充，1：快充

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal power; // 功率

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal serviceFee; // 服务费单价

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal peakPrice;    // 峰时，10:00~15:00, 18:00~21:00
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal normalPrice;  // 平时，7:00~10:00, 15:00~18:00, 21:00~23:00
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal valleyPrice;  // 谷时，23:00~7:00

    @Column(nullable = false)
    private Integer maxQueueLength; // 最大排队数

    @Transient
    private ChargingStationSlot slot;

    // 报表信息
    @Embedded
    private ReportInfo report;
}