package com.xiaozhuo.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.*;

/**
 * 日志工具类：统一管理日志配置和获取 Logger 实例
 */
public class LogUtil {

    private static boolean initialized = false;

    /**
     * 初始化日志系统
     * 加载 logging.properties 配置文件
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            // 确保 logs 目录存在
            ensureLogDirectoryExists();

            // 从 resources 目录加载日志配置文件
            InputStream is = LogUtil.class.getClassLoader().getResourceAsStream("logging.properties");

            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                System.out.println("Logging system initialized successfully");
            } else {
                System.err.println("WARNING: logging.properties not found, using default configuration");
                configureDefaultLogging();
            }

            initialized = true;

        } catch (IOException e) {
            System.err.println("Failed to initialize logging system: " + e.getMessage());
            configureDefaultLogging();
            initialized = true;
        }
    }

    /**
     * 确保日志目录存在
     */
    private static void ensureLogDirectoryExists() {
        File logDir = new File("logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                System.out.println("Created logs directory: " + logDir.getAbsolutePath());
            } else {
                System.err.println("WARNING: Failed to create logs directory");
            }
        }
    }

    /**
     * 配置默认日志（当配置文件不存在时使用）
     */
    private static void configureDefaultLogging() {
        // 获取根 Logger
        Logger rootLogger = Logger.getLogger("");

        // 清除默认处理器
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // 添加控制台处理器
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new LogFormatter());
        rootLogger.addHandler(consoleHandler);

        // 设置全局日志级别
        rootLogger.setLevel(Level.INFO);

        System.out.println("Default logging configuration applied");
    }

    /**
     * 获取 Logger 实例
     * @param clazz 类对象
     * @return Logger 实例
     */
    public static Logger getLogger(Class<?> clazz) {
        if (!initialized) {
            init();
        }
        return Logger.getLogger(clazz.getName());
    }

    /**
     * 获取 Logger 实例
     * @param name Logger 名称
     * @return Logger 实例
     */
    public static Logger getLogger(String name) {
        if (!initialized) {
            init();
        }
        return Logger.getLogger(name);
    }

    /**
     * 记录数据库连接日志
     * @param logger Logger 实例
     * @param message 日志消息
     */
    public static void logConnection(Logger logger, String message) {
        logger.fine("[DB Connection] " + message);
    }

    /**
     * 记录 SQL 执行日志
     * @param logger Logger 实例
     * @param sql SQL 语句
     * @param params 参数列表
     */
    public static void logSql(Logger logger, String sql, Object... params) {
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder("[SQL] ");
            sb.append(sql);

            if (params != null && params.length > 0) {
                sb.append(" | Params: ");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(params[i]);
                }
            }

            logger.fine(sb.toString());
        }
    }

    /**
     * 记录业务操作日志
     * @param logger Logger 实例
     * @param operation 操作名称
     * @param message 日志消息
     */
    public static void logBusiness(Logger logger, String operation, String message) {
        logger.info("[Business] " + operation + " - " + message);
    }

    /**
     * 记录异常日志
     * @param logger Logger 实例
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public static void logError(Logger logger, String message, Throwable throwable) {
        logger.log(Level.SEVERE, "[Error] " + message, throwable);
    }

    /**
     * 记录警告日志
     * @param logger Logger 实例
     * @param message 警告消息
     */
    public static void logWarning(Logger logger, String message) {
        logger.warning("[Warning] " + message);
    }

    /**
     * 记录事务日志
     * @param logger Logger 实例
     * @param message 事务消息
     */
    public static void logTransaction(Logger logger, String message) {
        logger.fine("[Transaction] " + message);
    }
}
