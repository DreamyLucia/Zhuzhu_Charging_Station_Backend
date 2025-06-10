package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;
import java.util.List;

@Data
public class ChargingStationSlot {
    // 实时状态
    private ChargingStationStatus status;
    // 等待完成时间
    private Long waitingTime;
    // 排队中的订单ID队列
    private List<String> queue;
}