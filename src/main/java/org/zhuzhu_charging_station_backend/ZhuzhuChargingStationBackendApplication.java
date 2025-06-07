package org.zhuzhu_charging_station_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZhuzhuChargingStationBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhuzhuChargingStationBackendApplication.class, args);
    }
}
