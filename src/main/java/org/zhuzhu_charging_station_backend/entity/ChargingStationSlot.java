package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;
import java.util.List;

@Data
public class ChargingStationSlot {
    // 实时状态
    private ChargingStationStatus status;
    // 排队中的订单ID队列
    private List<Long> queue;
}