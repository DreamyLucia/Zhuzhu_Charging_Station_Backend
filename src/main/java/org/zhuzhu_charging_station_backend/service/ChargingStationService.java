package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.zhuzhu_charging_station_backend.dto.ChargingStationResponse;
import org.zhuzhu_charging_station_backend.dto.ChargingStationUpsertRequest;
import org.zhuzhu_charging_station_backend.entity.*;
import org.zhuzhu_charging_station_backend.exception.AlreadyExistsException;
import org.zhuzhu_charging_station_backend.repository.ChargingStationRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.zhuzhu_charging_station_backend.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class ChargingStationService {

    @Autowired
    private ApplicationContext applicationContext;
    private final ChargingStationRepository chargingStationRepository;
    private final ChargingStationSlotService chargingStationSlotService;

    /**
     * 新增或更新充电桩基础信息，并同步维护实时状态与报表数据。
     * @param request 充电桩创建/更新请求，id为空为新增，否则为修改
     * @return 充电桩完整信息（含基础属性、slot、报表信息）
     */
    @Transactional
    @CacheEvict(value = {"chargingStationBase", "chargingStationResponsesAllId"}, allEntries = true)
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
            station.setPeakPrice(request.getPeakPrice());
            station.setNormalPrice(request.getNormalPrice());
            station.setValleyPrice(request.getValleyPrice());
            station.setMaxQueueLength(request.getMaxQueueLength());

            // 初始化报表对象ReportInfo
            ReportInfo report = new ReportInfo();
            report.setUpdatedAt(LocalDateTime.now());
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
            chargingStationSlotService.setSlot(id, slot);

            return buildChargingStationResponse(saved, slot);
        } else {
            // 修改流程
            id = request.getId();
            station = chargingStationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("充电桩不存在"));

            // 用分布式锁包裹所有的“能否修改”检查和slot修改
            chargingStationSlotService.updateSlotWithLock(id, slot -> {
                ChargingStationStatus status = slot.getStatus();
                if (status == null) {
                    throw new IllegalStateException("充电桩实时状态初始化失败");
                }
                // 充电桩"使用中"禁止一切变更（基础信息+slot状态）
                if (status.getStatus() != null && status.getStatus() == 1) {
                    throw new AlreadyExistsException("充电桩正在使用中，禁止修改所有信息");
                }
                // 可以修改slot状态
                if (request.getStatus() != null) {
                    status.setStatus(request.getStatus());
                    if (request.getStatus() == 2) { // 2 = 关闭
                        status.setCurrentChargeCount(0);
                        status.setCurrentChargeTime(0L);
                        status.setCurrentChargeAmount(0D);
                    }
                }
                slot.setStatus(status);

                // 可以改数据库基础属性
                if (request.getName() != null) station.setName(request.getName());
                if (request.getDescription() != null) station.setDescription(request.getDescription());
                if (request.getMode() != null) station.setMode(request.getMode());
                if (request.getPower() != null) station.setPower(request.getPower());
                if (request.getServiceFee() != null) station.setServiceFee(request.getServiceFee());
                if (request.getPeakPrice() != null) station.setPeakPrice(request.getPeakPrice());
                if (request.getNormalPrice() != null) station.setNormalPrice(request.getNormalPrice());
                if (request.getValleyPrice() != null) station.setValleyPrice(request.getValleyPrice());
                if (request.getMaxQueueLength() != null) station.setMaxQueueLength(request.getMaxQueueLength());

                // 只有允许修改时才做save
                chargingStationRepository.save(station);
            });

            ChargingStationSlot slot = chargingStationSlotService.getSlot(id);
            return buildChargingStationResponse(station, slot);
        }
    }

    /**
     * 删除充电桩及其Slot数据
     * @param id 充电桩ID
     */
    @Transactional
    @CacheEvict(value = {"chargingStationBase", "chargingStationResponsesAllId"}, allEntries = true)
    public void deleteChargingStation(Long id) {
        try {
            chargingStationRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("充电桩不存在，无法删除");
        }
        chargingStationSlotService.removeSlot(id);
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

        chargingStationSlotService.updateSlotWithLock(id, slot -> {
            ChargingStationStatus status = slot.getStatus();
            if (status == null) status = new ChargingStationStatus();
            status.setStatus(3); // 3=故障
            slot.setStatus(status);
        });

        ChargingStationSlot slot = chargingStationSlotService.getSlot(id);
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

        chargingStationSlotService.updateSlotWithLock(id, slot -> {
            ChargingStationStatus status = slot.getStatus();
            if (status == null) status = new ChargingStationStatus();
            status.setStatus(0); // 0=空闲
            slot.setStatus(status);
        });

        ChargingStationSlot slot = chargingStationSlotService.getSlot(id);
        return buildChargingStationResponse(station, slot);
    }

    /**
     * 查询所有充电桩的 ID 列表
     * @return 充电桩 ID 列表
     */
    @Cacheable(value = "chargingStationResponsesAllId")
    public List<Long> getAllStationIds() {
        return chargingStationRepository.findAll()
                .stream()
                .map(ChargingStation::getId)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定充电桩全部信息（含slot和报表信息）
     * @param id 充电桩ID
     * @return 充电桩静态响应对象
     */
    @Cacheable(value = "chargingStationBase", key = "#id")
    public ChargingStation getChargingStationBase(Long id) {
        return chargingStationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("充电桩不存在"));
    }

    /**
     * 查询指定充电桩全部信息（含slot和报表信息）
     * @param id 充电桩ID
     * @return 充电桩响应对象
     */
    public ChargingStationResponse getChargingStationWithSlot(Long id) {
        ChargingStationService proxy = applicationContext.getBean(ChargingStationService.class);
        ChargingStation station = proxy.getChargingStationBase(id); // 这样走代理，@Cacheable生效
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
     * 获取或初始化某个充电桩的slot信息（带默认状态）
     * @param id 充电桩ID
     * @return 充电桩slot
     */
    private ChargingStationSlot getOrInitSlot(Long id) {
        ChargingStationSlot slot = chargingStationSlotService.getSlot(id);
        boolean needSet = false;
        if (slot == null) {
            slot = new ChargingStationSlot();
            needSet = true;
        }
        if (slot.getStatus() == null) {
            ChargingStationStatus status = new ChargingStationStatus();
            status.setStatus(0);
            status.setCurrentChargeCount(0);
            status.setCurrentChargeTime(0L);
            status.setCurrentChargeAmount(0D);
            slot.setStatus(status);
            needSet = true;
        }
        if (slot.getQueue() == null) {
            slot.setQueue(new ArrayList<>());
            needSet = true;
        }
        if (needSet) {
            chargingStationSlotService.setSlot(id, slot);
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
                station.getPeakPrice(),
                station.getNormalPrice(),
                station.getValleyPrice(),
                station.getMaxQueueLength(),
                slot,
                station.getReport()
        );
    }
}