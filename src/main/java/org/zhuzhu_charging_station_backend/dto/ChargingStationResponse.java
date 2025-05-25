package org.zhuzhu_charging_station_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;
import org.zhuzhu_charging_station_backend.entity.ReportInfo;

@Data
@AllArgsConstructor
public class ChargingStationResponse {
    private Long id;
    private String name;
    private Integer mode;
    private Double power;
    private ChargingStationSlot slot; // 包含 status + queue
    private ReportInfo report; // 报表数据
}