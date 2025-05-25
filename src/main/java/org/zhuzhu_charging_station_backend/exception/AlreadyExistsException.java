package org.zhuzhu_charging_station_backend.exception;

/**
 * 资源已存在异常。用于新增时违反唯一约束等场景返回409。
 */
public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}