package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

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

    @Column(nullable = false)
    private Double power; // 功率

    @Column(nullable = false)
    private Double serviceFee; // 服务费单价

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "charging_station_unit_price",
            joinColumns = @JoinColumn(name = "station_id")
    )
    private List<UnitPricePeriod> unitPrices; // 电价时段数组

    @Column(nullable = false)
    private Integer maxQueueLength; // 新增：最大排队数

    @Transient
    private ChargingStationSlot slot;

    // 报表信息
    @Embedded
    private ReportInfo report;
}