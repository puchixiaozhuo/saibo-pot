package com.xiaozhuo.result;

import lombok.Data;

/**
 * 统一错误响应对象
 */
@Data
public class ErrorResponse {

    private int code;
    private String message;
    private String timestamp;
    private String path;

    public ErrorResponse() {
    }

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public ErrorResponse(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}
