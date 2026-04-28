package com.xiaozhuo.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 自定义数据库连接池实现
 * 使用线程安全的队列管理连接，支持连接的复用和自动回收
 */
public class ConnectionPool {

    private static final Logger logger = LogUtil.getLogger(ConnectionPool.class);


    // 空闲连接队列（线程安全）
    private static final ConcurrentLinkedQueue<Connection> idleConnections = new ConcurrentLinkedQueue<>();

    // 活跃连接计数器（原子操作，保证线程安全）
    private static final AtomicInteger activeCount = new AtomicInteger(0);

    // 连接池配置参数
    private static int coreSize;      // 核心连接数（最小连接数）
    private static int maxSize;       // 最大连接数
    private static long timeout;      // 获取连接超时时间（毫秒）

    // 静态初始化：在类加载时创建连接池
    static {
        initializePool();
    }

    /**
     * 初始化连接池
     * 从配置文件读取参数，并创建核心数量的连接
     */
    private static void initializePool() {
        try {
            // 读取配置参数
            coreSize = Integer.parseInt(JDBCUtil.getProperty("jdbc.pool.coreSize", "5"));
            maxSize = Integer.parseInt(JDBCUtil.getProperty("jdbc.pool.maxSize", "10"));
            timeout = Long.parseLong(JDBCUtil.getProperty("jdbc.pool.timeout", "3000"));

            logger.info("Initializing connection pool: coreSize=" + coreSize
                      + ", maxSize=" + maxSize
                      + ", timeout=" + timeout + "ms");

            // 预创建核心数量的连接
            for (int i = 0; i < coreSize; i++) {
                Connection conn = createNewConnection();
                if (conn != null) {
                    idleConnections.offer(conn);
                    logger.fine("Created initial connection #" + (i + 1));
                }
            }

            logger.info("Connection pool initialized successfully with "
                      + idleConnections.size() + " connections");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize connection pool", e);
            throw new RuntimeException("Connection pool initialization failed", e);
        }
    }

    /**
     * 创建新的数据库连接
     * @return 新的数据库连接对象，失败返回null
     */
    private static Connection createNewConnection() {
        try {
            Connection conn = JDBCUtil.getRawConnection();
            logger.fine("Created new database connection: " + conn.hashCode());
            return conn;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to create new connection", e);
            return null;
        }
    }

    /**
     * 从连接池获取连接（带超时控制）
     * @return 可用的数据库连接
     * @throws SQLException 获取连接失败或超时
     */
    public static Connection getConnection() throws SQLException {
        long startTime = System.currentTimeMillis();

        // 尝试从空闲队列获取连接
        Connection conn = idleConnections.poll();

        if (conn != null) {
            // 验证连接是否有效
            if (isConnectionValid(conn)) {
                activeCount.incrementAndGet();
                logger.fine("Connection retrieved from pool. Active: " + activeCount.get()
                          + ", Idle: " + idleConnections.size());
                return conn;
            } else {
                // 连接已失效，关闭并继续获取
                logger.warning("Found invalid connection in pool, closing it");
                closeQuietly(conn);
            }
        }

        // 空闲队列为空，检查是否可以创建新连接
        if (activeCount.get() < maxSize) {
            Connection newConn = createNewConnection();
            if (newConn != null) {
                activeCount.incrementAndGet();
                logger.fine("Created new connection. Active: " + activeCount.get());
                return newConn;
            }
        }

        // 达到最大连接数，等待其他连接释放（带超时）
        logger.warning("Connection pool exhausted, waiting for available connection...");
        while (System.currentTimeMillis() - startTime < timeout) {
            conn = idleConnections.poll();
            if (conn != null && isConnectionValid(conn)) {
                activeCount.incrementAndGet();
                logger.fine("Connection retrieved after waiting. Active: " + activeCount.get());
                return conn;
            }

            // 短暂休眠，避免CPU空转
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for connection", e);
            }
        }

        // 超时，抛出异常
        String errorMsg = String.format("Get connection timeout after %dms. Active: %d, Idle: %d",
                                      timeout, activeCount.get(), idleConnections.size());
        logger.severe(errorMsg);
        throw new SQLException(errorMsg);
    }

    /**
     * 归还连接到连接池
     * @param conn 要归还的连接
     */
    public static void returnConnection(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            // 检查连接是否有效
            if (!isConnectionValid(conn)) {
                logger.warning("Returning invalid connection, closing it");
                closeQuietly(conn);
                activeCount.decrementAndGet();
                return;
            }

            // 重置连接状态（防止事务未提交等问题）
            if (!conn.getAutoCommit()) {
                conn.setAutoCommit(true);
            }

            // 归还到空闲队列
            idleConnections.offer(conn);
            activeCount.decrementAndGet();

            logger.fine("Connection returned to pool. Active: " + activeCount.get()
                      + ", Idle: " + idleConnections.size());

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error returning connection to pool", e);
            closeQuietly(conn);
            activeCount.decrementAndGet();
        }
    }

    /**
     * 验证连接是否有效
     * @param conn 待验证的连接
     * @return true-有效，false-无效
     */
    private static boolean isConnectionValid(Connection conn) {
        if (conn == null) {
            return false;
        }

        try {
            // 使用isValid方法检查连接状态（JDBC 4.0+）
            return conn.isValid(2); // 2秒超时
        } catch (SQLException e) {
            logger.fine("Connection validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 安静地关闭连接（不抛出异常）
     * @param conn 要关闭的连接
     */
    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                logger.fine("Connection closed: " + conn.hashCode());
            } catch (SQLException e) {
                logger.log(Level.FINE, "Error closing connection", e);
            }
        }
    }

    /**
     * 获取当前活跃连接数
     * @return 活跃连接数量
     */
    public static int getActiveCount() {
        return activeCount.get();
    }

    /**
     * 获取当前空闲连接数
     * @return 空闲连接数量
     */
    public static int getIdleCount() {
        return idleConnections.size();
    }

    /**
     * 获取连接池状态信息
     * @return 状态描述字符串
     */
    public static String getPoolStatus() {
        return String.format("Pool Status - Active: %d, Idle: %d, Total: %d, Max: %d",
                           activeCount.get(), idleConnections.size(),
                           activeCount.get() + idleConnections.size(), maxSize);
    }

    /**
     * 关闭连接池（应用关闭时调用）
     */
    public static void shutdown() {
        logger.info("Shutting down connection pool...");

        Connection conn;
        int closedCount = 0;

        // 关闭所有空闲连接
        while ((conn = idleConnections.poll()) != null) {
            closeQuietly(conn);
            closedCount++;
        }

        logger.info("Connection pool shut down. Closed " + closedCount + " connections.");
    }
}
