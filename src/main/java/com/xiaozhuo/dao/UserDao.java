package com.xiaozhuo.dao;

import com.xiaozhuo.entity.User;

import java.sql.Connection;
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
     */
    User findByUsername(Connection conn, String username);

    /**
     * 插入新用户
     * @param conn 数据库连接
     * @param user 用户对象
     * @return 影响行数
     */
    int insert(Connection conn, User user);

    /**
     * 根据用户 ID 查询用户
     * @param conn 数据库连接
     * @param id 用户 ID
     * @return 用户对象，未找到返回 null
     */
    User findById(Connection conn, Long id);

    /**
     * 更新用户登录时间
     * @param conn 数据库连接
     * @param userId 用户 ID
     * @param lastLoginTime 上次登录时间
     */
    void updateLoginTime(Connection conn, Long userId, LocalDateTime lastLoginTime);
}
