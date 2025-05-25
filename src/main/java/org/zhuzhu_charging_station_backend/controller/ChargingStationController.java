package org.zhuzhu_charging_station_backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.*;
import org.zhuzhu_charging_station_backend.dto.*;
import org.zhuzhu_charging_station_backend.entity.*;
import org.zhuzhu_charging_station_backend.service.ChargingStationService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/charging-stations")
@Tag(name = "充电桩管理")
public class ChargingStationController {
    private final ChargingStationService chargingStationService;

    public ChargingStationController(ChargingStationService chargingStationService) {
        this.chargingStationService = chargingStationService;
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有充电桩的完整信息")
    public StandardResponse<List<ChargingStationResponse>> getAllStations() {
        return StandardResponse.success(chargingStationService.getAllChargingStationWithSlot());
    }

    @GetMapping("/{id}/slot")
    @Operation(summary = "获取某个充电桩的完整信息")
    public StandardResponse<ChargingStationResponse> getStation(@PathVariable Long id) {
        return StandardResponse.success(chargingStationService.getChargingStationWithSlot(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除充电桩")
    public StandardResponse<Void> delete(@PathVariable Long id) {
        chargingStationService.deleteChargingStation(id);
        return StandardResponse.success();
    }

    @PostMapping("/upsert")
    @Operation(summary = "新增或修改充电桩（ID为空为新增，否则为更新）")
    public StandardResponse<ChargingStationResponse> upsert(@RequestBody ChargingStationUpsertRequest request) {
        return StandardResponse.success(chargingStationService.upsertChargingStation(request));
    }

    @PostMapping("/{id}/repair")
    @Operation(summary = "维修充电桩")
    public StandardResponse<Void> repair(@PathVariable Long id) {
        chargingStationService.repairChargingStation(id);
        return StandardResponse.success();
    }
}