package com.xiaozhuo.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志工具类
 * 考核要求：使用JDK自带java.util.logging，统一记录不同级别日志
 */
public class LogUtil {
    // 私有化构造方法，禁止实例化
    private LogUtil() {}

    /**
     * 获取当前调用类的Logger
     */
    private static Logger getLogger() {
        // 获取调用该方法的类名
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stack[2];
        return Logger.getLogger(caller.getClassName());
    }

    /**
     * 调试日志（开发时用，记录详细流程）
     * @param msg 日志信息
     */
    public static void debug(String msg) {
        getLogger().log(Level.FINE, msg);
    }

    /**
     * 信息日志（常规操作，如数据库连接、接口调用成功）
     * @param msg 日志信息
     */
    public static void info(String msg) {
        getLogger().log(Level.INFO, msg);
    }

    /**
     * 警告日志（非致命错误，如参数不合法、缓存过期）
     * @param msg 日志信息
     */
    public static void warn(String msg) {
        getLogger().log(Level.WARNING, msg);
    }

    /**
     * 错误日志（致命错误，如数据库连接失败、异常抛出）
     * @param msg 日志信息
     * @param e 异常对象
     */
    public static void error(String msg, Exception e) {
        getLogger().log(Level.SEVERE, msg, e);
    }

    /**
     * 错误日志（重载，无异常对象）
     * @param msg 日志信息
     */
    public static void error(String msg) {
        getLogger().log(Level.SEVERE, msg);
    }

    // 测试主方法
    public static void main(String[] args) {
        LogUtil.debug("调试：用户开始登录");
        LogUtil.info("信息：用户登录成功，用户ID：1");
        LogUtil.warn("警告：Token即将过期，剩余10分钟");
        try {
            int a = 1 / 0;
        } catch (Exception e) {
            LogUtil.error("错误：计算异常", e);
        }
    }
}