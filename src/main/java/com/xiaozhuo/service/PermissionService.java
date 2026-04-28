package com.xiaozhuo.service;

import com.xiaozhuo.result.Result;

import java.util.Set;

public interface PermissionService {
    /**
     * 获取用户权限列表
     * @param userId 用户ID
     * @return 权限集合
     */
    Result<Set<String>> getUserPermissions(Long userId);

    /**
     * 检查用户是否有指定权限
     * @param userId 用户ID
     * @param permCode 权限代码
     * @return true-有权限，false-无权限
     */
    boolean hasPermission(Long userId, String permCode);

    /**
     * 检查用户是否有任一权限
     * @param userId 用户ID
     * @param permCodes 权限代码数组
     * @return true-有任一权限，false-无任何权限
     */
    boolean hasAnyPermission(Long userId, String... permCodes);

    /**
     * 检查用户是否有所有权限
     * @param userId 用户ID
     * @param permCodes 权限代码数组
     * @return true-有所有权限，false-缺少部分权限
     */
    boolean hasAllPermissions(Long userId, String... permCodes);

    /**
     * 刷新用户权限缓存
     * @param userId 用户ID
     */
    void refreshUserPermissions(Long userId);
}
