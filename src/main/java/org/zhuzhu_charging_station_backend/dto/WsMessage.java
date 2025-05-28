package org.zhuzhu_charging_station_backend.dto;

import lombok.Data;

@Data
public class WsMessage<T> {
    /**
     * 消息类型, 如 upsert, cancel, finish, query 等
     */
    private String type;
    /**
     * 对应业务数据
     */
    private T data;
}