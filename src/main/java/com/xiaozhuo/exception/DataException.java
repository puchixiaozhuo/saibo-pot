package com.xiaozhuo.exception;

/**
 * 数据异常（验证失败、数据不存在等）
 */
public class DataException extends BusinessException {

    public DataException(String message) {
        super(404, message);
    }

    public DataException(String message, Throwable cause) {
        super(404, message, cause);
    }
}
