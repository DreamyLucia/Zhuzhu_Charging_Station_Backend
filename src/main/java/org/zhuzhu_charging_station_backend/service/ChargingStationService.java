package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.zhuzhu_charging_station_backend.dto.ChargingStationResponse;
import org.zhuzhu_charging_station_backend.dto.ChargingStationUpsertRequest;
import org.zhuzhu_charging_station_backend.entity.ChargingStation;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;
import org.zhuzhu_charging_station_backend.entity.ChargingStationStatus;
import org.zhuzhu_charging_station_backend.entity.ReportInfo;
import org.zhuzhu_charging_station_backend.exception.AlreadyExistsException;
import org.zhuzhu_charging_station_backend.repository.ChargingStationRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.zhuzhu_charging_station_backend.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class ChargingStationService {

    private final ChargingStationRepository chargingStationRepository;
    private final RedisTemplate<String, ChargingStationSlot> slotRedisTemplate;

    /**
     * 新增或更新充电桩基础信息，并同步维护实时状态与报表数据。
     * @param request 充电桩创建/更新请求，id为空为新增，否则为修改
     * @return 充电桩完整信息（含基础属性、slot、报表信息）
     */
    public ChargingStationResponse upsertChargingStation(ChargingStationUpsertRequest request) {
        ChargingStation station;
        boolean isCreate = (request.getId() == null);
        Long id;

        if (isCreate) {
            if (chargingStationRepository.existsByName(request.getName())) {
                throw new AlreadyExistsException("充电桩名称已存在，请更换");
            }

            // 创建流程
            id = IdGenerator.generateUniqueStationId(chargingStationRepository, 10);
            station = new ChargingStation();
            station.setId(id);
            station.setName(request.getName());
            station.setMode(request.getMode());
            station.setPower(request.getPower());

            // 初始化报表对象ReportInfo
            ReportInfo report = new ReportInfo();
            report.setTotalChargeCount(0);
            report.setTotalChargeTime(0L);
            report.setTotalChargeAmount(0D);
            report.setTotalChargeFee(0D);
            report.setTotalServiceFee(0D);
            report.setTotalFee(0D);
            station.setReport(report);

            ChargingStation saved = chargingStationRepository.save(station);

            // 初始化slot记录
            ChargingStationStatus status = new ChargingStationStatus();
            status.setStatus(0); // 强制设为0
            status.setCurrentChargeCount(0);
            status.setCurrentChargeTime(0L);
            status.setCurrentChargeAmount(0D);

            ChargingStationSlot slot = new ChargingStationSlot();
            slot.setStatus(status);
            slot.setQueue(new ArrayList<>());
            slotRedisTemplate.opsForValue().set(slotKey(id), slot);

            return new ChargingStationResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getMode(),
                    saved.getPower(),
                    slot,
                    saved.getReport()
            );
        } else {
            // 修改流程
            id = request.getId();
            station = chargingStationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("充电桩不存在"));
            if (request.getName() != null) station.setName(request.getName());
            if (request.getMode() != null) station.setMode(request.getMode());
            if (request.getPower() != null) station.setPower(request.getPower());
            ChargingStation saved = chargingStationRepository.save(station);

            // 获取并更新slot
            ChargingStationSlot slot = slotRedisTemplate.opsForValue().get(slotKey(id));
            if (slot == null) {
                // 若Redis未命中，初始化slot
                ChargingStationStatus status = new ChargingStationStatus();
                status.setStatus(0);
                status.setCurrentChargeCount(0);
                status.setCurrentChargeTime(0L);
                status.setCurrentChargeAmount(0D);
                slot = new ChargingStationSlot();
                slot.setStatus(status);
                slot.setQueue(new ArrayList<>());
            }
            // 更新slot状态
            ChargingStationStatus status = slot.getStatus();
            if (status == null) {
                // 若未初始化，补全默认值
                status = new ChargingStationStatus();
                status.setStatus(0);
                status.setCurrentChargeCount(0);
                status.setCurrentChargeTime(0L);
                status.setCurrentChargeAmount(0D);
            }
            if (request.getStatus() != null) {
                status.setStatus(request.getStatus());
                if (request.getStatus() == 2) { // 2 = 关闭，重置实时统计计数
                    status.setCurrentChargeCount(0);
                    status.setCurrentChargeTime(0L);
                    status.setCurrentChargeAmount(0D);
                }
            }
            slot.setStatus(status);
            slotRedisTemplate.opsForValue().set(slotKey(id), slot);

            return new ChargingStationResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getMode(),
                    saved.getPower(),
                    slot,
                    saved.getReport()
            );
        }
    }

    /**
     * 删除充电桩及其Slot数据
     * @param id 充电桩ID
     */
    public void deleteChargingStation(Long id) {
        try {
            chargingStationRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("充电桩不存在，无法删除");
        }
        slotRedisTemplate.delete(slotKey(id));
    }

    /**
     * 将充电桩状态切为“空闲”，用于维修/复位场景
     * @param id 充电桩ID
     */
    public void repairChargingStation(Long id) {
        if (!chargingStationRepository.existsById(id)) {
            throw new NotFoundException("充电桩不存在，无法维修");
        }
        ChargingStationSlot slot = slotRedisTemplate.opsForValue().get(slotKey(id));
        if (slot == null) {
            slot = new ChargingStationSlot();
            slot.setQueue(new ArrayList<>());
            slot.setStatus(new ChargingStationStatus());
        }
        ChargingStationStatus status = slot.getStatus();
        if (status == null) {
            status = new ChargingStationStatus();
        }
        status.setStatus(0); // 0=空闲
        slot.setStatus(status);
        slotRedisTemplate.opsForValue().set(slotKey(id), slot);
    }

    /**
     * 查询指定充电桩全部信息（含slot和报表信息）
     * @param id 充电桩ID
     * @return 充电桩响应对象
     */
    public ChargingStationResponse getChargingStationWithSlot(Long id) {
        ChargingStation station = chargingStationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("充电桩不存在"));
        ChargingStationSlot slot = slotRedisTemplate.opsForValue().get(slotKey(id));
        return new ChargingStationResponse(
                station.getId(),
                station.getName(),
                station.getMode(),
                station.getPower(),
                slot,
                station.getReport()
        );
    }

    /**
     * 查询所有充电桩完整信息列表（含slot和报表信息）
     * @return 全部充电桩响应对象列表
     */
    public List<ChargingStationResponse> getAllChargingStationWithSlot() {
        List<ChargingStation> stations = chargingStationRepository.findAll();
        return stations.stream()
                .map(station -> {
                    ChargingStationSlot slot = slotRedisTemplate.opsForValue().get(slotKey(station.getId()));
                    return new ChargingStationResponse(
                            station.getId(),
                            station.getName(),
                            station.getMode(),
                            station.getPower(),
                            slot,
                            station.getReport()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 生成Slot在Redis中的唯一键
     * @param id 充电桩ID
     * @return Redis key
     */
    private String slotKey(Long id) {
        return "charging_station_slot:" + id;
    }
}