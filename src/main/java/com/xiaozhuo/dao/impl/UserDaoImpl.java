package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.util.JDBCUtil;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * 用户数据访问实现类
 */
public class UserDaoImpl implements UserDao {

    @Override
    public User findByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT id, username, password, salt, nickname, avatar, role, status, create_time, update_time " +
                    "FROM sys_user WHERE username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setSalt(rs.getString("salt"));
                    user.setNickname(rs.getString("nickname"));
                    user.setAvatar(rs.getString("avatar"));
                    user.setRole(rs.getInt("role"));
                    user.setStatus(rs.getInt("status"));

                    // MySQL 8.0 的 LocalDateTime 支持
                    Timestamp createTimeTs = rs.getTimestamp("create_time");
                    if (createTimeTs != null) {
                        user.setCreateTime(createTimeTs.toLocalDateTime());
                    }

                    Timestamp updateTimeTs = rs.getTimestamp("update_time");
                    if (updateTimeTs != null) {
                        user.setUpdateTime(updateTimeTs.toLocalDateTime());
                    }

                    return user;
                }
            }
        }
        return null;
    }

    @Override
    public int insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO sys_user (username, password, salt, nickname, avatar, role, status, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getSalt());
            pstmt.setString(4, user.getNickname());
            pstmt.setString(5, user.getAvatar());
            pstmt.setInt(6, user.getRole());
            pstmt.setInt(7, user.getStatus());

            // LocalDateTime 转 Timestamp
            if (user.getCreateTime() != null) {
                pstmt.setTimestamp(8, Timestamp.valueOf(user.getCreateTime()));
            } else {
                pstmt.setTimestamp(8, null);
            }

            if (user.getUpdateTime() != null) {
                pstmt.setTimestamp(9, Timestamp.valueOf(user.getUpdateTime()));
            } else {
                pstmt.setTimestamp(9, null);
            }

            int rows = pstmt.executeUpdate();

            // 获取自动生成的主键 ID
            if (rows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        user.setId(generatedId); // 将生成的 ID 设置回 user 对象
                    }
                }
            }

            return rows;
        }
    }

    @Override
    public User findById(Connection conn, Long id) throws SQLException {
        String sql = "SELECT id, username, password, salt, nickname, avatar, role, status, create_time, update_time " +
                "FROM sys_user WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setSalt(rs.getString("salt"));
                    user.setNickname(rs.getString("nickname"));
                    user.setAvatar(rs.getString("avatar"));
                    user.setRole(rs.getInt("role"));
                    user.setStatus(rs.getInt("status"));

                    // MySQL 8.0 的 LocalDateTime 支持
                    Timestamp createTimeTs = rs.getTimestamp("create_time");
                    if (createTimeTs != null) {
                        user.setCreateTime(createTimeTs.toLocalDateTime());
                    }

                    Timestamp updateTimeTs = rs.getTimestamp("update_time");
                    if (updateTimeTs != null) {
                        user.setUpdateTime(updateTimeTs.toLocalDateTime());
                    }

                    return user;
                }
            }
        }
        return null;
    }
    @Override
    public void updateLoginTime(Connection conn, Long userId, LocalDateTime lastLoginTime) throws SQLException {
        String sql = "UPDATE sys_user SET update_time = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (lastLoginTime != null) {
                pstmt.setTimestamp(1, Timestamp.valueOf(lastLoginTime));
            } else {
                pstmt.setTimestamp(1, null);
            }
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        }
    }
}

