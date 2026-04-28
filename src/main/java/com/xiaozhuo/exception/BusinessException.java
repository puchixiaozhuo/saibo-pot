package com.xiaozhuo.exception;

/**
 * 自定义异常基类
 * 所有业务异常都继承此类
 */
public class BusinessException extends RuntimeException {

    private final int errorCode;
    private final String errorMessage;

    public BusinessException(String message) {
        super(message);
        this.errorCode = 500;
        this.errorMessage = message;
    }

    public BusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 500;
        this.errorMessage = message;
    }

    public BusinessException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
