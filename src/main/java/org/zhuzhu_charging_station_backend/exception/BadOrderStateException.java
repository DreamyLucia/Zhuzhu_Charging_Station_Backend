package org.zhuzhu_charging_station_backend.exception;

public class BadOrderStateException extends RuntimeException {
    public BadOrderStateException(String message) {
        super(message);
    }
}