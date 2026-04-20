package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.PermissionDao;
import com.xiaozhuo.entity.Permission;
import com.xiaozhuo.util.JDBCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class PermissionDaoImpl implements PermissionDao {

    @Override
    public Set<String> selectPermCodesByUserId(Connection conn, Long userId) throws Exception {
        String sql = "SELECT DISTINCT p.perm_code FROM sys_permission p " +
                "INNER JOIN sys_role_perm rp ON p.id = rp.perm_id " +
                "INNER JOIN sys_user u ON u.role = rp.role " +
                "WHERE u.id = ? AND u.status = 1";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Set<String> permCodes = new HashSet<>();

        try {
            if (conn == null) {
                conn = JDBCUtil.getConnection();
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                permCodes.add(rs.getString("perm_code"));
            }

            return permCodes;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Set<String> selectPermCodesByRole(Connection conn, Integer role) throws Exception {
        String sql = "SELECT DISTINCT p.perm_code FROM sys_permission p " +
                "INNER JOIN sys_role_perm rp ON p.id = rp.perm_id " +
                "WHERE rp.role = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Set<String> permCodes = new HashSet<>();

        try {
            if (conn == null) {
                conn = JDBCUtil.getConnection();
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, role);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                permCodes.add(rs.getString("perm_code"));
            }

            return permCodes;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Permission> selectAllPermissions(Connection conn) throws Exception {
        String sql = "SELECT * FROM sys_permission ORDER BY id";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Permission> permissions = new ArrayList<>();

        try {
            if (conn == null) {
                conn = JDBCUtil.getConnection();
            }

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Permission permission = new Permission();
                permission.setId(rs.getLong("id"));
                permission.setPermCode(rs.getString("perm_code"));
                permission.setPermName(rs.getString("perm_name"));
                permission.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                permissions.add(permission);
            }

            return permissions;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}