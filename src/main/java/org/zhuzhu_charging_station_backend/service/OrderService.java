package org.zhuzhu_charging_station_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhuzhu_charging_station_backend.dto.OrderUpsertRequest;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.exception.BadStateException;
import org.zhuzhu_charging_station_backend.repository.OrderRepository;
import org.zhuzhu_charging_station_backend.util.IdGenerator;
import org.zhuzhu_charging_station_backend.util.JwtTokenUtil;
import org.zhuzhu_charging_station_backend.exception.NotFoundException;
import org.zhuzhu_charging_station_backend.exception.ForbiddenException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final QueueService queueService;
    private final OrderCacheService orderCacheService;
    private final OrderRepository orderRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final ChargingStationService chargingStationService;

    /**
     * 新建或修改订单，自动分配（新）排队号并存入redis
     */
    public Order upsertOrder(OrderUpsertRequest req, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order;
        boolean isNew = (req.getId() == null);

        if (isNew) {
            long orderId = IdGenerator.generateUniqueOrderId(orderRepository, 10);
            order = new Order();
            order.setId(orderId);
            order.setUserId(userId);
            order.setRecordTime(LocalDateTime.now());
            order.setStatus(3);
        } else {
            order = orderCacheService.getOrder(req.getId());
            if (order == null) {
                throw new NotFoundException("订单不存在！");
            }
            if (!userId.equals(order.getUserId())) {
                throw new ForbiddenException("无权限操作他人订单！");
            }
            queueService.removeOrderFromQueueWithLock(order.getMode(), String.valueOf(order.getId()));
        }
        order.setMode(req.getMode());
        order.setChargeAmount(req.getChargeAmount());
        order.setQueueNo(queueService.assignQueueNoWithLock(order.getMode(), order.getMode() == 1 ? "F" : "T"));
        queueService.addOrderToQueueWithLock(order.getMode(), String.valueOf(order.getId()));
        orderCacheService.saveOrder(order);
        return order;
    }

    /**
     * 查询订单
     */
    public Order getOrder(Long orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            // Cache未命中，去数据库查
            order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new NotFoundException("订单不存在！");
            }
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限查询他人订单！");
        }
        return order;
    }

    /**
     * 取消订单（从队列和Redis移除，并入库标记为已取消）
     */
    public void cancelOrder(Long orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在！");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限取消他人订单！");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new BadStateException("订单当前状态不可取消！");
        }
        queueService.removeOrderFromQueueWithLock(order.getMode(), String.valueOf(order.getId()));
        order.setStatus(4); // 4: 已取消
        orderRepository.save(order);
        orderCacheService.deleteOrder(orderId);
    }

    /**
     * 完结订单（从队列移除，更新订单状态和充电细节）
     */
    public Order finishOrder(Long orderId, String token) {
        Long userId = jwtTokenUtil.extractUserId(token);
        Order order = orderCacheService.getOrder(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在！");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("无权限操作他人订单！");
        }
        if (order.getStatus() != 1) {
            throw new BadStateException("订单当前状态不可完结！");
        }
        queueService.removeOrderFromQueueWithLock(order.getMode(), String.valueOf(order.getId()));
        // 设置完结的业务字段
        order.setStatus(0); // 0:已完成
        order.setStopTime(LocalDateTime.now());
        orderRepository.save(order);
        orderCacheService.deleteOrder(orderId);
        chargingStationService.updateReportInfo(order.getChargingStationId(), order);
        return order;
    }
}