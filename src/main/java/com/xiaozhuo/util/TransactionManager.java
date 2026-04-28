package com.xiaozhuo.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 事务管理器：统一管理数据库事务的开始、提交、回滚
 * 使用 ThreadLocal 确保同一线程内共享同一个连接和事务状态
 */
public class TransactionManager {

    private static final Logger logger = LogUtil.getLogger(TransactionManager.class);

    // 使用 ThreadLocal 绑定当前线程的数据库连接
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    // 使用 ThreadLocal 跟踪事务是否已开启
    private static final ThreadLocal<Boolean> transactionStarted = ThreadLocal.withInitial(() -> false);

    /**
     * 开启事务
     * 从连接池获取连接并设置 autoCommit 为 false
     * @throws SQLException SQL 异常
     */
    public static void beginTransaction() throws SQLException {
        // 检查是否已经开启了事务
        if (transactionStarted.get()) {
            logger.warning("Transaction already started in current thread");
            return;
        }

        // 从连接池获取连接
        Connection conn = ConnectionPool.getConnection();

        // 关闭自动提交，开启事务
        conn.setAutoCommit(false);

        // 绑定到当前线程
        connectionHolder.set(conn);
        transactionStarted.set(true);

        LogUtil.logTransaction(logger, "Transaction started");
    }

    /**
     * 提交事务
     * 提交当前事务并释放连接
     * @throws SQLException SQL 异常
     */
    public static void commit() throws SQLException {
        Connection conn = connectionHolder.get();

        if (conn == null) {
            throw new SQLException("No transaction started, cannot commit");
        }

        try {
            conn.commit();
            LogUtil.logTransaction(logger, "Transaction committed successfully");
        } catch (SQLException e) {
            LogUtil.logError(logger, "Failed to commit transaction", e);
            throw e;
        } finally {
            // 释放资源
            cleanup();
        }
    }

    /**
     * 回滚事务
     * 回滚当前事务并释放连接
     */
    public static void rollback() {
        Connection conn = connectionHolder.get();

        if (conn == null) {
            logger.warning("No transaction started, nothing to rollback");
            return;
        }

        try {
            conn.rollback();
            LogUtil.logTransaction(logger, "Transaction rolled back");
        } catch (SQLException e) {
            LogUtil.logError(logger, "Failed to rollback transaction", e);
        } finally {
            // 释放资源
            cleanup();
        }
    }

    /**
     * 获取当前线程的数据库连接
     * 如果在事务中，返回事务连接；否则从连接池获取新连接
     * @return 数据库连接
     */
    public static Connection getConnection() {
        try {
            Connection conn = connectionHolder.get();

            if (conn != null) {
                // 在事务中，返回事务连接
                LogUtil.logTransaction(logger, "Using transactional connection");
                return conn;
            }

            // 不在事务中，从连接池获取新连接
            LogUtil.logTransaction(logger, "Getting new connection from pool");
            return ConnectionPool.getConnection();

        } catch (SQLException e) {
            LogUtil.logError(logger, "Failed to get database connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    /**
     * 清理 ThreadLocal 变量，释放资源
     * 将连接归还到连接池
     */
    private static void cleanup() {
        Connection conn = connectionHolder.get();

        if (conn != null) {
            try {
                // 恢复 autoCommit 状态
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }

                // 归还连接到连接池
                ConnectionPool.returnConnection(conn);
                LogUtil.logTransaction(logger, "Connection returned to pool");
            } catch (SQLException e) {
                LogUtil.logError(logger, "Failed to cleanup connection", e);
            }
        }

        // 清除 ThreadLocal
        connectionHolder.remove();
        transactionStarted.remove();
    }

    /**
     * 检查当前线程是否有活跃事务
     * @return true-有事务，false-无事务
     */
    public static boolean hasActiveTransaction() {
        return transactionStarted.get() && connectionHolder.get() != null;
    }

    /**
     * 执行带事务的操作（函数式接口）
     * 自动管理事务的开始、提交和回滚
     *
     * @param action 要执行的操作
     * @throws Exception 操作异常
     */
    public static void executeWithTransaction(TransactionAction action) throws Exception {
        try {
            beginTransaction();
            action.execute();
            commit();
        } catch (Exception e) {
            rollback();
            LogUtil.logError(logger, "Transaction failed, rolled back", e);
            throw e;
        }
    }

    /**
     * 函数式接口：定义事务内执行的操作
     */
    @FunctionalInterface
    public interface TransactionAction {
        void execute() throws Exception;
    }
}