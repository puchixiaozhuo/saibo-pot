package com.xiaozhuo.service.Impl;

import com.xiaozhuo.dao.PermissionDao;
import com.xiaozhuo.dao.impl.PermissionDaoImpl;
import com.xiaozhuo.service.PermissionService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.PermissionCacheUtil;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PermissionServiceImpl implements PermissionService {

    private PermissionDao permissionDao = new PermissionDaoImpl();

    @Override
    public Map<String, Object> getUserPermissions(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;

        try {
            conn = JDBCUtil.getConnection();

            Set<String> permissions = PermissionCacheUtil.getUserPermissions(userId);

            if (permissions == null || permissions.isEmpty()) {
                permissions = permissionDao.selectPermCodesByUserId(conn, userId);
                PermissionCacheUtil.setUserPermissions(userId, permissions);
            }

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", permissions);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }

        return result;
    }

    @Override
    public boolean hasPermission(Long userId, String permCode) {
        Connection conn = null;

        try {
            conn = JDBCUtil.getConnection();

            Set<String> permissions = PermissionCacheUtil.getUserPermissions(userId);

            if (permissions == null || permissions.isEmpty()) {
                permissions = permissionDao.selectPermCodesByUserId(conn, userId);
                PermissionCacheUtil.setUserPermissions(userId, permissions);
            }

            return permissions.contains(permCode);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            JDBCUtil.close(conn, null);
        }
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permCodes) {
        Connection conn = null;

        try {
            conn = JDBCUtil.getConnection();

            Set<String> permissions = PermissionCacheUtil.getUserPermissions(userId);

            if (permissions == null || permissions.isEmpty()) {
                permissions = permissionDao.selectPermCodesByUserId(conn, userId);
                PermissionCacheUtil.setUserPermissions(userId, permissions);
            }

            for (String permCode : permCodes) {
                if (permissions.contains(permCode)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            JDBCUtil.close(conn, null);
        }
    }

    @Override
    public boolean hasAllPermissions(Long userId, String... permCodes) {
        Connection conn = null;

        try {
            conn = JDBCUtil.getConnection();

            Set<String> permissions = PermissionCacheUtil.getUserPermissions(userId);

            if (permissions == null || permissions.isEmpty()) {
                permissions = permissionDao.selectPermCodesByUserId(conn, userId);
                PermissionCacheUtil.setUserPermissions(userId, permissions);
            }

            for (String permCode : permCodes) {
                if (!permissions.contains(permCode)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            JDBCUtil.close(conn, null);
        }
    }

    @Override
    public void refreshUserPermissions(Long userId) {
        PermissionCacheUtil.clearUserPermissions(userId);
    }
}