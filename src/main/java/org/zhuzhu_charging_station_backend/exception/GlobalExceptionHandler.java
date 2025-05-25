package org.zhuzhu_charging_station_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;

/**
 * 全局异常处理器，统一格式化所有Controller抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获查无/删无等资源不存在异常，返回404
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardResponse<?> handleNotFoundException(NotFoundException e) {
        return StandardResponse.error(404, e.getMessage());
    }

    /**
     * 兜底处理其他异常，返回500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public StandardResponse<?> handleException(Exception e) {
        return StandardResponse.error(500, "服务器内部错误: " + e.getMessage());
    }
}