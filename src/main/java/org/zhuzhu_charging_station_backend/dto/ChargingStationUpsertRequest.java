package org.zhuzhu_charging_station_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargingStationUpsertRequest {
    private Long id;                             // ID，null 表示创建，否则为修改
    private String name;                         // 名字
    private String description;                  // 充电桩描述
    private Integer mode;                        // 充电模式
    private Double power;                        // 功率
    private Double serviceFee;                   // 服务费单价
    private Double peakPrice;                    // 峰时，10:00~15:00, 18:00~21:00
    private Double normalPrice;                  // 平时，7:00~10:00, 15:00~18:00, 21:00~23:00
    private Double valleyPrice;                  // 谷时，23:00~7:00
    private Integer maxQueueLength;              // 最大排队数
    private Integer status;                      // 充电桩状态
}