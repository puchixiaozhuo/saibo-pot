package com.xiaozhuo.dao;

import com.xiaozhuo.entity.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 用户数据访问接口
 */
public interface UserDao {

    /**
     * 根据用户名查询用户
     * @param conn 数据库连接
     * @param username 用户名
     * @return 用户对象，未找到返回 null
     * @throws SQLException SQL 异常
     */
    User findByUsername(Connection conn, String username) throws SQLException;

    /**
     * 插入新用户
     * @param conn 数据库连接
     * @param user 用户对象
     * @return 影响行数
     * @throws SQLException SQL 异常
     */
    int insert(Connection conn, User user) throws SQLException;

    /**
     * 根据用户 ID 查询用户
     * @param conn 数据库连接
     * @param id 用户 ID
     * @return 用户对象，未找到返回 null
     * @throws SQLException SQL 异常
     */
    User findById(Connection conn, Long id) throws SQLException;

    /**
     * 更新用户登录时间
     * @param conn 数据库连接
     * @param userId 用户 ID
     * @param lastLoginTime 上次登录时间
     * @throws SQLException SQL 异常
     */
    void updateLoginTime(Connection conn, Long userId, LocalDateTime lastLoginTime) throws SQLException;

}

