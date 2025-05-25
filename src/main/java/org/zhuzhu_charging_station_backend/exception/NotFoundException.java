package org.zhuzhu_charging_station_backend.exception;

/**
 * 资源不存在异常。用于查无/删无等场景返回404。
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}