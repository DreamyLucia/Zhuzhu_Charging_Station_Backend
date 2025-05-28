package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class Order {

    @Id
    private Long id; // 16位详单ID

    @Column(nullable = false)
    private Long userId; // 用户ID

    @Column(nullable = true)
    private Long chargingStationId; // 充电桩ID（可空，排队时未分配）

    @Column(nullable = false)
    private Integer mode; // 充电模式，0:慢充，1:快充

    @Column(nullable = false, length = 10)
    private String queueNo; // 排队号 F1/T2 等

    @Column(nullable = false)
    private LocalDateTime recordTime; // 详单生成时间

    @Column(nullable = false)
    private Integer status; // 订单状态，0:已完成，1:进行中，2：排队中，3：已取消

    @Column(nullable = true)
    private Double chargeAmount; // 充电电量

    @Column(nullable = true)
    private Long chargeDuration; // 充电时长

    @Column(nullable = true)
    private LocalDateTime startTime; // 启动时间

    @Column(nullable = true)
    private LocalDateTime stopTime; // 停止时间

    @Column(nullable = true)
    private Double chargeFee; // 充电费用

    @Column(nullable = true)
    private Double serviceFee; // 服务费用

    @Column(nullable = true)
    private Double totalFee; // 总费用
}