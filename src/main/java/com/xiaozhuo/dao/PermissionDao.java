package com.xiaozhuo.dao;

import com.xiaozhuo.entity.Permission;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

public interface PermissionDao {
    /**
     * 根据用户ID查询权限编码列表
     */
    Set<String> selectPermCodesByUserId(Connection conn, Long userId) throws Exception;

    /**
     * 根据角色查询权限编码列表
     */
    Set<String> selectPermCodesByRole(Connection conn, Integer role) throws Exception;

    /**
     * 查询所有权限
     */
    List<Permission> selectAllPermissions(Connection conn) throws Exception;
}