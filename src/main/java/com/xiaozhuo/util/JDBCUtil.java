package com.xiaozhuo.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC工具类：加载配置、获取连接、关闭资源
 */
public class JDBCUtil {
    // 1. 定义配置变量，从db.properties读取
    private static String driver;
    private static String url;
    private static String username;
    private static String password;

    // 2. 静态代码块：类加载时执行，只加载一次配置、注册驱动
    static {
        try {
            // 加载db.properties配置文件（从resources目录读取）
            Properties prop = new Properties();
            // 用类加载器读取资源文件，避免路径问题
            InputStream is = JDBCUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (is == null) {
                throw new IOException("db.properties配置文件未找到！");
            }
            prop.load(is);

            // 读取配置
            driver = prop.getProperty("jdbc.driver");
            url = prop.getProperty("jdbc.url");
            username = prop.getProperty("jdbc.username");
            password = prop.getProperty("jdbc.password");

            // 注册驱动（MySQL8.0+ 可省略，但保留兼容性）
            Class.forName(driver);
            System.out.println("JDBC工具类初始化成功，驱动加载完成！");
        } catch (Exception e) {
            e.printStackTrace();
            // 初始化失败，抛出异常，避免后续空指针
            throw new ExceptionInInitializerError("JDBC工具类初始化失败：" + e.getMessage());
        }
    }

    /**
     * 获取数据库连接
     * @return 数据库连接对象
     * @throws SQLException 连接失败抛出异常
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }


    /**
     * 获取原始数据库连接（用于连接池内部创建新连接）
     * @return 数据库连接对象
     * @throws SQLException 连接失败抛出异常
     */
    public static Connection getRawConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 从连接池获取数据库连接（推荐使用）
     * @return 数据库连接对象
     * @throws SQLException 连接失败抛出异常
     */
    public static Connection getConnectionFromPool() throws SQLException {
        return ConnectionPool.getConnection();
    }

    /**
     * 归还连接到连接池
     * @param conn 要归还的连接
     */
    public static void returnToPool(Connection conn) {
        ConnectionPool.returnConnection(conn);
    }

    /**
     * 获取配置属性值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static String getProperty(String key, String defaultValue) {
        try {
            Properties prop = new Properties();
            InputStream is = JDBCUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (is == null) {
                return defaultValue;
            }
            prop.load(is);
            String value = prop.getProperty(key);
            return value != null ? value : defaultValue;
        } catch (IOException e) {
            return defaultValue;
        }
    }

    /**
     * 关闭资源（重载：关闭连接、Statement、ResultSet）
     * @param conn 连接
     * @param stmt 语句对象
     * @param rs 结果集
     */
    public static void close(Connection conn, java.sql.Statement stmt, java.sql.ResultSet rs) {
        // 逆序关闭，避免空指针
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭资源（重载：仅关闭连接、Statement）
     * @param conn 连接
     * @param stmt 语句对象
     */
    public static void close(Connection conn, java.sql.Statement stmt) {
        close(conn, stmt, null);
    }
}