package org.zhuzhu_charging_station_backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UnitPricePeriod implements Serializable {
    private Double price;         // 单价

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;  // 起始时间

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;    // 结束时间
}