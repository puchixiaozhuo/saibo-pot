
package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.PermissionDao;
import com.xiaozhuo.entity.Permission;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * 权限数据访问实现类
 */
public class PermissionDaoImpl implements PermissionDao {

    private static final Logger logger = LogUtil.getLogger(PermissionDaoImpl.class);

    /**
     * 根据用户ID查询权限代码集合
     */
    @Override
    public Set<String> selectPermCodesByUserId(Connection conn, Long userId) {
        String sql = "SELECT DISTINCT p.perm_code FROM sys_permission p " +
                "INNER JOIN sys_role_perm rp ON p.id = rp.perm_id " +
                "INNER JOIN sys_user u ON u.role = rp.role " +
                "WHERE u.id = ? AND u.status = 1";

        Set<String> permCodes = new HashSet<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permCodes.add(rs.getString("perm_code"));
                }
            }

            return permCodes;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询用户权限失败: userId=" + userId, e);
            throw new DatabaseException("查询用户权限失败", e);
        }
    }

    /**
     * 根据角色查询权限代码集合
     */
    @Override
    public Set<String> selectPermCodesByRole(Connection conn, Integer role) {
        String sql = "SELECT DISTINCT p.perm_code FROM sys_permission p " +
                "INNER JOIN sys_role_perm rp ON p.id = rp.perm_id " +
                "WHERE rp.role = ?";

        Set<String> permCodes = new HashSet<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, role);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permCodes.add(rs.getString("perm_code"));
                }
            }

            return permCodes;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询角色权限失败: role=" + role, e);
            throw new DatabaseException("查询角色权限失败", e);
        }
    }

    /**
     * 查询所有权限
     */
    @Override
    public List<Permission> selectAllPermissions(Connection conn) {
        String sql = "SELECT * FROM sys_permission ORDER BY id";

        List<Permission> permissions = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Permission permission = new Permission();
                permission.setId(rs.getLong("id"));
                permission.setPermCode(rs.getString("perm_code"));
                permission.setPermName(rs.getString("perm_name"));
                permission.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                permissions.add(permission);
            }

            return permissions;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询所有权限失败", e);
            throw new DatabaseException("查询所有权限失败", e);
        }
    }
}
