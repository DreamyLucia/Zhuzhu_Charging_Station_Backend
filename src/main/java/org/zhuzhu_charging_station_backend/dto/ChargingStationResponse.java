package org.zhuzhu_charging_station_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;
import org.zhuzhu_charging_station_backend.entity.ReportInfo;
import org.zhuzhu_charging_station_backend.entity.UnitPricePeriod;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ChargingStationResponse {
    private LocalDateTime queriedAt;             // 本次响应的查询时间
    private Long id;
    private String name;
    private Integer mode;
    private Double power;
    private Double serviceFee;                   // 服务费单价
    private List<UnitPricePeriod> unitPrices;    // 电价时段数组
    private Integer maxQueueLength;              // 最大排队数
    private ChargingStationSlot slot;            // 包含 实时状态+队列
    private ReportInfo report;                   // 报表数据
}