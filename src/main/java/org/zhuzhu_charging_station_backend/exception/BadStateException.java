package org.zhuzhu_charging_station_backend.exception;

public class BadStateException extends RuntimeException {
    public BadStateException(String message) {
        super(message);
    }
}