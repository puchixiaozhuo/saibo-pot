package com.xiaozhuo.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限缓存工具类
 * 使用内存缓存存储用户权限，避免频繁查询数据库
 */
public class PermissionCacheUtil {

    private static final ConcurrentHashMap<Long, Set<String>> USER_PERMISSIONS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Set<String>> ROLE_PERMISSIONS_CACHE = new ConcurrentHashMap<>();

    private static final long CACHE_EXPIRE_TIME = 30 * 60 * 1000; // 30分钟过期
    private static final ConcurrentHashMap<Long, Long> CACHE_EXPIRE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取用户权限列表（带缓存）
     */
    public static Set<String> getUserPermissions(Long userId) {
        Long expireTime = CACHE_EXPIRE_MAP.get(userId);
        if (expireTime != null && System.currentTimeMillis() > expireTime) {
            USER_PERMISSIONS_CACHE.remove(userId);
            CACHE_EXPIRE_MAP.remove(userId);
        }
        return USER_PERMISSIONS_CACHE.get(userId);
    }

    /**
     * 设置用户权限缓存
     */
    public static void setUserPermissions(Long userId, Set<String> permissions) {
        USER_PERMISSIONS_CACHE.put(userId, permissions);
        CACHE_EXPIRE_MAP.put(userId, System.currentTimeMillis() + CACHE_EXPIRE_TIME);
    }

    /**
     * 清除用户权限缓存
     */
    public static void clearUserPermissions(Long userId) {
        USER_PERMISSIONS_CACHE.remove(userId);
        CACHE_EXPIRE_MAP.remove(userId);
    }

    /**
     * 获取角色权限列表
     */
    public static Set<String> getRolePermissions(Integer role) {
        return ROLE_PERMISSIONS_CACHE.get(role);
    }

    /**
     * 设置角色权限缓存
     */
    public static void setRolePermissions(Integer role, Set<String> permissions) {
        ROLE_PERMISSIONS_CACHE.put(role, permissions);
    }

    /**
     * 清除所有缓存
     */
    public static void clearAll() {
        USER_PERMISSIONS_CACHE.clear();
        ROLE_PERMISSIONS_CACHE.clear();
        CACHE_EXPIRE_MAP.clear();
    }
}
