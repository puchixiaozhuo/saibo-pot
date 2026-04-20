package com.xiaozhuo.service;

import java.util.Map;
import java.util.Set;

public interface PermissionService {
    /**
     * 获取用户权限列表
     */
    Map<String, Object> getUserPermissions(Long userId);

    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(Long userId, String permCode);

    /**
     * 检查用户是否有任一权限
     */
    boolean hasAnyPermission(Long userId, String... permCodes);

    /**
     * 检查用户是否有所有权限
     */
    boolean hasAllPermissions(Long userId, String... permCodes);

    /**
     * 刷新用户权限缓存
     */
    void refreshUserPermissions(Long userId);
}