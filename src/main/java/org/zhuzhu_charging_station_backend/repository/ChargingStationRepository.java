package org.zhuzhu_charging_station_backend.repository;

import org.zhuzhu_charging_station_backend.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
    boolean existsByName(String name);
    // JpaRepository 已有 boolean existsById(Long id)
}