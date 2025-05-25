package org.zhuzhu_charging_station_backend.repository;

import org.zhuzhu_charging_station_backend.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
    // JpaRepository 已有 boolean existsById(Long id)

    // 自定义按名字查重
    boolean existsByName(String name);
}