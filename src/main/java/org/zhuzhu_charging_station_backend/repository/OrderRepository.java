package org.zhuzhu_charging_station_backend.repository;

import org.zhuzhu_charging_station_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    // JpaRepository 已有 existsById(String id)
    // 查找指定充电桩ID的所有详单
    List<Order> findByChargingStationId(Long chargingStationId);

    // 查找指定用户ID的所有详单
    List<Order> findByUserId(Long userId);
}