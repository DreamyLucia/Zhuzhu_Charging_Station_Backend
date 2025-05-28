package org.zhuzhu_charging_station_backend.exception;

/**
 * 业务无权限异常（用于鉴权失败时主动抛出，返回403）
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}