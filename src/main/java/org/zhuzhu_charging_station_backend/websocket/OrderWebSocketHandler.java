package org.zhuzhu_charging_station_backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.zhuzhu_charging_station_backend.dto.OrderUpsertRequest;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;
import org.zhuzhu_charging_station_backend.entity.Order;
import org.zhuzhu_charging_station_backend.service.OrderService;
import org.zhuzhu_charging_station_backend.service.OrderService.FinishInfo;

@Component
@RequiredArgsConstructor
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.get("type").asText();
            String token = root.get("token").asText();

            if ("upsert".equals(type)) {
                OrderUpsertRequest req = objectMapper.treeToValue(root.get("data"), OrderUpsertRequest.class);
                Order order = orderService.upsertOrder(req, token);
                sendResponse(session, StandardResponse.success(order));
            } else if ("cancel".equals(type)) {
                Long orderId = root.get("data").get("orderId").asLong();
                orderService.cancelOrder(orderId, token);
                sendResponse(session, StandardResponse.success("订单已取消"));
            } else if ("finish".equals(type)) {
                Long orderId = root.get("data").get("orderId").asLong();
                FinishInfo finishInfo = objectMapper.treeToValue(root.get("data").get("finishInfo"), FinishInfo.class);
                orderService.finishOrder(orderId, token, finishInfo);
                sendResponse(session, StandardResponse.success("订单已结束"));
            } else {
                sendResponse(session, StandardResponse.error(400, "未知type: " + type));
            }

        } catch (Exception e) {
            sendResponse(session, StandardResponse.error(500, "内部错误: " + e.getMessage()));
        }
    }

    // 工具方法：回复前端
    private void sendResponse(WebSocketSession session, StandardResponse<?> resp) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        } catch (Exception ignored) {}
    }
}