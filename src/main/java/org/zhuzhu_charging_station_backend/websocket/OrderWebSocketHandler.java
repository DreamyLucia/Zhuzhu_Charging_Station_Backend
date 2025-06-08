package org.zhuzhu_charging_station_backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.zhuzhu_charging_station_backend.dto.OrderUpsertRequest;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.exception.BadStateException;
import org.zhuzhu_charging_station_backend.service.OrderService;
import org.zhuzhu_charging_station_backend.service.UserService;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    // 用于管理每个session的定时任务
    private final ConcurrentMap<WebSocketSession, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.get("type").asText();
            String token = (String) session.getAttributes().get("token");

            if ("upsert".equals(type)) {
                OrderUpsertRequest req = objectMapper.treeToValue(root.get("data"), OrderUpsertRequest.class);
                Order order = orderService.upsertOrder(req, token);
                session.getAttributes().put("orderId", order.getId());
                startPushTask(session, order.getId(), token); // 开启推送
            } else if ("query".equals(type)) {
                Long orderId = root.get("data").get("orderId").asLong();
                Order order = orderService.getOrder(orderId, token);
                session.getAttributes().put("orderId", orderId);
                startPushTask(session, order.getId(), token); // 开启推送
            } else if ("cancel".equals(type)) {
                Long orderId = (Long) session.getAttributes().get("orderId");
                if (orderId == null) {
                    throw new BadStateException("未指定订单");
                }
                orderService.cancelOrder(orderId, token);
                sendResponse(session, StandardResponse.success("订单已取消"));
                stopPushTask(session);
                session.close();
            } else if ("finish".equals(type)) {
                Long orderId = (Long) session.getAttributes().get("orderId");
                if (orderId == null) {
                    throw new BadStateException("未指定订单");
                }
                Order order = orderService.finishOrder(orderId, token);
                sendResponse(session, StandardResponse.success(order));
                stopPushTask(session);
                session.close();
            } else {
                throw new BadStateException("未知type: " + type);
            }

        } catch (BadStateException e) {
            sendResponse(session, StandardResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            sendResponse(session, StandardResponse.error(500, "内部错误: " + e.getMessage()));
        }
    }

    // 开始定时推送
    private void startPushTask(WebSocketSession session, Long orderId, String token) {
        // 有旧的先停
        stopPushTask(session);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                Order order = orderService.getOrder(orderId, token);
                sendResponse(session, StandardResponse.success(order)); // 发送订单状态
                if (order.getStatus() == 0) {
                    stopPushTask(session);
                    session.close();
                }
            } catch (Exception e) {
                sendResponse(session, StandardResponse.error(500, "推送订单失败: " + e.getMessage()));
                stopPushTask(session);
            }
        }, 0, 1, TimeUnit.SECONDS);
        taskMap.put(session, future);
    }

    // 停止定时推送
    private void stopPushTask(WebSocketSession session) {
        ScheduledFuture<?> future = taskMap.remove(session);
        if (future != null) future.cancel(true);
    }

    // 客户端断线时也停止推送，避免泄露
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        stopPushTask(session);
    }

    // 工具方法：回复前端
    private void sendResponse(WebSocketSession session, StandardResponse<?> resp) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        } catch (Exception ignored) {}
    }
}