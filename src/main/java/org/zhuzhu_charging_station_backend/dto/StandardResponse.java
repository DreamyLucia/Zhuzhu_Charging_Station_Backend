package org.zhuzhu_charging_station_backend.dto;

public class StandardResponse<T> {
    private int code;
    private String msg;
    private T data;

    // 成功响应构造方法
    public static <T> StandardResponse<T> success(T data) {
        return new StandardResponse<>(200, "Success", data);
    }

    // 错误响应构造方法
    public static <T> StandardResponse<T> error(int code, String msg) {
        return new StandardResponse<>(code, msg, null);
    }

    // 全参数构造方法
    public StandardResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // Getter方法
    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
}
