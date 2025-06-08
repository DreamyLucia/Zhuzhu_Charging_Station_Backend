package org.zhuzhu_charging_station_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;
import org.zhuzhu_charging_station_backend.entity.ReportInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargingStationResponse {
    private LocalDateTime queriedAt;             // 本次响应的查询时间
    private Long id;
    private String name;
    private String description;                  // 充电桩描述
    private Integer mode;
    private BigDecimal power;
    private BigDecimal serviceFee;                   // 服务费单价
    private BigDecimal peakPrice;                    // 峰时，10:00~15:00, 18:00~21:00
    private BigDecimal normalPrice;                  // 平时，7:00~10:00, 15:00~18:00, 21:00~23:00
    private BigDecimal valleyPrice;                  // 谷时，23:00~7:00
    private Integer maxQueueLength;              // 最大排队数
    private ChargingStationSlot slot;            // 包含 实时状态+队列
    private ReportInfo report;                   // 报表数据
}