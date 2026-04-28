package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.dao.PermissionDao;
import com.xiaozhuo.dao.impl.PermissionDaoImpl;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.PermissionService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.PermissionCacheUtil;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限业务实现类（优化版）
 * 使用 IoC 容器管理 + 权限缓存
 */
@Bean
public class PermissionServiceImpl implements PermissionService {

    private PermissionDao permissionDao = new PermissionDaoImpl();

    /**
     * 获取用户权限列表
     */
    @Override
    public Result<Set<String>> getUserPermissions(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            Set<String> permissions = PermissionCacheUtil.getUserPermissions(userId);

            if (permissions == null || permissions.isEmpty()) {
                permissions = permissionDao.selectPermCodesByUserId(conn, userId);
                PermissionCacheUtil.setUserPermissions(userId, permissions);
            }

            return Result.success("查询成功", permissions);

        } catch (Exception e) {
            throw new BusinessException(500, "查询权限失败：" + e.getMessage());
        } finally {
            if (conn != null) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 检查用户是否有指定权限
     */
    @Override
    public boolean hasPermission(Long userId, String permCode) {
        if (userId == null || permCode == null || permCode.isEmpty()) {
            return false;
        }

        Set<String> permissions = getOrLoadPermissions(userId);
        return permissions.contains(permCode);
    }

    /**
     * 检查用户是否有任一权限
     */
    @Override
    public boolean hasAnyPermission(Long userId, String... permCodes) {
        if (userId == null || permCodes == null || permCodes.length == 0) {
            return false;
        }

        Set<String> permissions = getOrLoadPermissions(userId);

        for (String permCode : permCodes) {
            if (permissions.contains(permCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查用户是否有所有权限
     */
    @Override
    public boolean hasAllPermissions(Long userId, String... permCodes) {
        if (userId == null || permCodes == null || permCodes.length == 0) {
            return false;
        }

        Set<String> permissions = getOrLoadPermissions(userId);

        for (String permCode : permCodes) {
            if (!permissions.contains(permCode)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 刷新用户权限缓存
     */
    @Override
    public void refreshUserPermissions(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }

        PermissionCacheUtil.clearUserPermissions(userId);
    }

    /**
     * 获取或加载用户权限（带缓存）
     */
    private Set<String> getOrLoadPermissions(Long userId) {
        Set<String> permissions = PermissionCacheUtil.getUserPermissions(userId);

        if (permissions == null || permissions.isEmpty()) {
            Connection conn = null;
            try {
                conn = JDBCUtil.getConnection();
                permissions = permissionDao.selectPermCodesByUserId(conn, userId);
                PermissionCacheUtil.setUserPermissions(userId, permissions);
            } catch (Exception e) {
                e.printStackTrace();
                return new HashSet<>();
            } finally {
                if (conn != null) {
                    JDBCUtil.returnToPool(conn);
                }
            }
        }

        return permissions;
    }
}

