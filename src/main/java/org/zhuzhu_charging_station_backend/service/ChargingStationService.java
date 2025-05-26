package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;
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
    @Transactional
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
            station.setDescription(request.getDescription());
            station.setMode(request.getMode());
            station.setPower(request.getPower());
            station.setServiceFee(request.getServiceFee());
            station.setUnitPrices(request.getUnitPrices());
            station.setMaxQueueLength(request.getMaxQueueLength());

            // 初始化报表对象ReportInfo
            ReportInfo report = new ReportInfo();
            report.setUpdatedAt(LocalDateTime.now().withNano(0));
            report.setTotalChargeCount(0);
            report.setTotalChargeTime(0L);
            report.setTotalChargeAmount(0D);
            report.setTotalChargeFee(0D);
            report.setTotalServiceFee(0D);
            report.setTotalFee(0D);
            station.setReport(report);

            ChargingStation saved = chargingStationRepository.save(station);

            // 初始化slot记录
            ChargingStationSlot slot = getOrInitSlot(id);
            slotRedisTemplate.opsForValue().set(slotKey(id), slot);

            return buildChargingStationResponse(saved, slot);
        } else {
            // 修改流程
            id = request.getId();
            station = chargingStationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("充电桩不存在"));

            // 限制：充电桩在使用中不可更改信息
            ChargingStationSlot slot = getOrInitSlot(id);
            ChargingStationStatus status = slot.getStatus();
            if (status == null) {
                throw new IllegalStateException("充电桩实时状态初始化失败");
            }
            if (status.getStatus() != null && status.getStatus() == 1) {
                throw new AlreadyExistsException("充电桩正在使用中，禁止修改信息");
            }

            // 基础信息更新
            if (request.getName() != null) station.setName(request.getName());
            if (request.getDescription() != null) station.setDescription(request.getDescription());
            if (request.getMode() != null) station.setMode(request.getMode());
            if (request.getPower() != null) station.setPower(request.getPower());
            if(request.getServiceFee()!=null) station.setServiceFee(request.getServiceFee());
            if(request.getUnitPrices()!=null) station.setUnitPrices(request.getUnitPrices());
            if(request.getMaxQueueLength()!=null) station.setMaxQueueLength(request.getMaxQueueLength());
            ChargingStation saved = chargingStationRepository.save(station);

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

            return buildChargingStationResponse(saved, slot);
        }
    }

    /**
     * 删除充电桩及其Slot数据
     * @param id 充电桩ID
     */
    @Transactional
    public void deleteChargingStation(Long id) {
        try {
            chargingStationRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("充电桩不存在，无法删除");
        }
        slotRedisTemplate.delete(slotKey(id));
    }

    /**
     * 将充电桩状态切为“故障”，用于故障模拟场景。
     * @param id 充电桩ID
     * @return 充电桩完整信息（含基础属性、slot、报表信息）
     */
    @Transactional
    public ChargingStationResponse breakChargingStation(Long id) {
        ChargingStation station = chargingStationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("充电桩不存在，无法置为故障"));

        ChargingStationSlot slot = getOrInitSlot(id);
        ChargingStationStatus status = slot.getStatus();
        status.setStatus(3); // 3=故障
        slot.setStatus(status);
        slotRedisTemplate.opsForValue().set(slotKey(id), slot);

        return buildChargingStationResponse(station, slot);
    }

    /**
     * 将充电桩状态切为“空闲”，用于维修/复位场景
     * @param id 充电桩ID
     * @return 充电桩完整信息（含基础属性、slot、报表信息）
     */
    @Transactional
    public ChargingStationResponse repairChargingStation(Long id) {
        ChargingStation station = chargingStationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("充电桩不存在，无法维修"));

        ChargingStationSlot slot = getOrInitSlot(id);
        ChargingStationStatus status = slot.getStatus();
        status.setStatus(0); // 0=空闲
        slot.setStatus(status);
        slotRedisTemplate.opsForValue().set(slotKey(id), slot);

        return buildChargingStationResponse(station, slot);
    }

    /**
     * 查询所有充电桩的 ID 列表
     * @return 充电桩 ID 列表
     */
    public List<Long> getAllStationIds() {
        return chargingStationRepository.findAll()
                .stream()
                .map(ChargingStation::getId)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定充电桩全部信息（含slot和报表信息）
     * @param id 充电桩ID
     * @return 充电桩响应对象
     */
    public ChargingStationResponse getChargingStationWithSlot(Long id) {
        ChargingStation station = chargingStationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("充电桩不存在"));
        ChargingStationSlot slot = getOrInitSlot(id);

        return buildChargingStationResponse(station, slot);
    }

    /**
     * 查询所有充电桩完整信息列表（含slot和报表信息）
     * @return 全部充电桩响应对象列表
     */
    public List<ChargingStationResponse> getAllChargingStationWithSlot() {
        List<ChargingStation> stations = chargingStationRepository.findAll();
        return stations.stream()
                .map(station -> {
                    ChargingStationSlot slot = getOrInitSlot(station.getId());

                    return buildChargingStationResponse(station, slot);
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

    /**
     * 获取或初始化某个充电桩的slot信息（带默认状态）
     * @param id 充电桩ID
     * @return 充电桩slot
     */
    private ChargingStationSlot getOrInitSlot(Long id) {
        ChargingStationSlot slot = slotRedisTemplate.opsForValue().get(slotKey(id));
        if (slot == null) {
            slot = new ChargingStationSlot();
        }
        if (slot.getStatus() == null) {
            ChargingStationStatus status = new ChargingStationStatus();
            status.setStatus(0);
            status.setCurrentChargeCount(0);
            status.setCurrentChargeTime(0L);
            status.setCurrentChargeAmount(0D);
            slot.setStatus(status);
        }
        if (slot.getQueue() == null) {
            slot.setQueue(new ArrayList<>());
        }
        return slot;
    }

    private ChargingStationResponse buildChargingStationResponse(
            ChargingStation station,
            ChargingStationSlot slot
    ) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return new ChargingStationResponse(
                now,
                station.getId(),
                station.getName(),
                station.getDescription(),
                station.getMode(),
                station.getPower(),
                station.getServiceFee(),
                station.getUnitPrices(),
                station.getMaxQueueLength(),
                slot,
                station.getReport()
        );
    }
}