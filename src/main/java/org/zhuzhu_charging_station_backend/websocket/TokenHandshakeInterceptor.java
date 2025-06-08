package org.zhuzhu_charging_station_backend.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手阶段，将header中的token添加到session attributes中
 */
public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = request.getHeaders().getFirst("Authorization");
        if (token != null && !token.isEmpty()) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7); // 从第八个字符开始就是token主体
            }
            attributes.put("token", token);
            return true;
        } else {
            if (response instanceof org.springframework.http.server.ServletServerHttpResponse) {
                ((org.springframework.http.server.ServletServerHttpResponse) response)
                        .getServletResponse().setStatus(401);
            }
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 不需要处理
    }
}