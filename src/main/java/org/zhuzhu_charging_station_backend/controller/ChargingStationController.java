package org.zhuzhu_charging_station_backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.*;
import org.zhuzhu_charging_station_backend.dto.*;
import org.zhuzhu_charging_station_backend.service.ChargingStationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/charging-stations")
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

    @GetMapping("/ids")
    @Operation(summary = "获取所有充电桩的ID列表")
    public StandardResponse<List<Long>> getAllStationIds() {
        return StandardResponse.success(chargingStationService.getAllStationIds());
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

    @PutMapping("/{id}/break")
    @Operation(summary = "模拟充电桩故障（只能改变status为故障，不能修改其他信息）")
    public StandardResponse<ChargingStationResponse> breakStation(@PathVariable Long id) {
        ChargingStationResponse response = chargingStationService.breakChargingStation(id);
        return StandardResponse.success(response);
    }

    @PutMapping("/{id}/reset")
    @Operation(summary = "开启/维修充电桩")
    public StandardResponse<ChargingStationResponse> repair(@PathVariable Long id) {
        ChargingStationResponse response = chargingStationService.resetChargingStationStatusToIdle(id);
        return StandardResponse.success(response);
    }
}