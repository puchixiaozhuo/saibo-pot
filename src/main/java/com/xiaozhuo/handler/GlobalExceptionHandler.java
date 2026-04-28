package com.xiaozhuo.handler;

import com.xiaozhuo.exception.*;
import com.xiaozhuo.result.ErrorResponse;
import com.xiaozhuo.util.JsonUtil;
import com.xiaozhuo.util.LogUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，返回标准化错误响应
 */
public class GlobalExceptionHandler {

    private static final Logger logger = LogUtil.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * @param response HTTP 响应
     * @param exception 业务异常
     * @param path 请求路径
     */
    public static void handleBusinessException(HttpServletResponse response,
                                              BusinessException exception,
                                              String path) {
        logger.warning("Business exception occurred: " + exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            exception.getErrorMessage(),
            path
        );

        sendErrorResponse(response, errorResponse, exception.getErrorCode());
    }

    /**
     * 处理用户异常
     * @param response HTTP 响应
     * @param exception 用户异常
     * @param path 请求路径
     */
    public static void handleUserException(HttpServletResponse response,
                                          UserException exception,
                                          String path) {
        logger.warning("User exception occurred: " + exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            exception.getErrorMessage(),
            path
        );

        sendErrorResponse(response, errorResponse, 400);
    }

    /**
     * 处理认证异常
     * @param response HTTP 响应
     * @param exception 认证异常
     * @param path 请求路径
     */
    public static void handleAuthException(HttpServletResponse response,
                                          AuthException exception,
                                          String path) {
        logger.warning("Auth exception occurred: " + exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            exception.getErrorMessage(),
            path
        );

        sendErrorResponse(response, errorResponse, 401);
    }

    /**
     * 处理权限异常
     * @param response HTTP 响应
     * @param exception 权限异常
     * @param path 请求路径
     */
    public static void handlePermissionException(HttpServletResponse response,
                                                PermissionException exception,
                                                String path) {
        logger.warning("Permission exception occurred: " + exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            exception.getErrorMessage(),
            path
        );

        sendErrorResponse(response, errorResponse, 403);
    }

    /**
     * 处理数据异常
     * @param response HTTP 响应
     * @param exception 数据异常
     * @param path 请求路径
     */
    public static void handleDataException(HttpServletResponse response,
                                          DataException exception,
                                          String path) {
        logger.warning("Data exception occurred: " + exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            exception.getErrorMessage(),
            path
        );

        sendErrorResponse(response, errorResponse, 404);
    }

    /**
     * 处理视频异常
     * @param response HTTP 响应
     * @param exception 视频异常
     * @param path 请求路径
     */
    public static void handleVideoException(HttpServletResponse response,
                                           VideoException exception,
                                           String path) {
        logger.warning("Video exception occurred: " + exception.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            exception.getErrorMessage(),
            path
        );

        sendErrorResponse(response, errorResponse, 400);
    }

    /**
     * 处理数据库异常
     * @param response HTTP 响应
     * @param exception 数据库异常
     * @param path 请求路径
     */
    public static void handleDatabaseException(HttpServletResponse response,
                                              DatabaseException exception,
                                              String path) {
        LogUtil.logError(logger, "Database exception occurred", exception);

        ErrorResponse errorResponse = new ErrorResponse(
            exception.getErrorCode(),
            "Database operation failed",
            path
        );

        sendErrorResponse(response, errorResponse, 500);
    }

    /**
     * 处理 SQL 异常
     * @param response HTTP 响应
     * @param exception SQL 异常
     * @param path 请求路径
     */
    public static void handleSQLException(HttpServletResponse response,
                                         SQLException exception,
                                         String path) {
        LogUtil.logError(logger, "SQL exception occurred", exception);

        ErrorResponse errorResponse = new ErrorResponse(
            500,
            "Database error occurred",
            path
        );

        sendErrorResponse(response, errorResponse, 500);
    }

    /**
     * 处理通用异常
     * @param response HTTP 响应
     * @param exception 通用异常
     * @param path 请求路径
     */
    public static void handleGenericException(HttpServletResponse response,
                                             Exception exception,
                                             String path) {
        LogUtil.logError(logger, "Unexpected exception occurred", exception);

        ErrorResponse errorResponse = new ErrorResponse(
            500,
            "Internal server error",
            path
        );

        sendErrorResponse(response, errorResponse, 500);
    }

    /**
     * 发送错误响应
     * @param response HTTP 响应
     * @param errorResponse 错误响应对象
     * @param httpStatus HTTP 状态码
     */
    private static void sendErrorResponse(HttpServletResponse response,
                                         ErrorResponse errorResponse,
                                         int httpStatus) {
        try {
            response.setStatus(httpStatus);
            response.setContentType("application/json;charset=UTF-8");

            PrintWriter writer = response.getWriter();
            writer.write(JsonUtil.toJson(errorResponse));
            writer.flush();
        } catch (IOException e) {
            LogUtil.logError(logger, "Failed to send error response", e);
        }
    }

    /**
     * 根据异常类型分发到对应的处理方法
     * @param response HTTP 响应
     * @param exception 异常对象
     * @param path 请求路径
     */
    public static void handleException(HttpServletResponse response,
                                      Exception exception,
                                      String path) {
        if (exception instanceof UserException) {
            handleUserException(response, (UserException) exception, path);
        } else if (exception instanceof AuthException) {
            handleAuthException(response, (AuthException) exception, path);
        } else if (exception instanceof PermissionException) {
            handlePermissionException(response, (PermissionException) exception, path);
        } else if (exception instanceof DataException) {
            handleDataException(response, (DataException) exception, path);
        } else if (exception instanceof VideoException) {
            handleVideoException(response, (VideoException) exception, path);
        } else if (exception instanceof DatabaseException) {
            handleDatabaseException(response, (DatabaseException) exception, path);
        } else if (exception instanceof BusinessException) {
            handleBusinessException(response, (BusinessException) exception, path);
        } else if (exception instanceof SQLException) {
            handleSQLException(response, (SQLException) exception, path);
        } else {
            handleGenericException(response, exception, path);
        }
    }
}
