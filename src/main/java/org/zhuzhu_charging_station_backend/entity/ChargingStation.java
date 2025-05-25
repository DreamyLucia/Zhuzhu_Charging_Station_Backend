package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

import javax.persistence.*;

@Data
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

    // 充电桩实时状态信息（从 Redis 查询）
    @Transient
    private ChargingStationStatus statusInfo;

    // 报表信息
    @Embedded
    private ReportInfo report;
}