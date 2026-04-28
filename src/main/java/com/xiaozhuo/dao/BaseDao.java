package com.xiaozhuo.dao;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 基础 DAO 接口：提供通用的 CRUD 操作
 * @param <T> 实体类型
 */
public interface BaseDao<T> {

    /**
     * 插入记录
     * @param conn 数据库连接
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(Connection conn, T entity);

    /**
     * 根据 ID 删除记录
     * @param conn 数据库连接
     * @param id 主键 ID
     * @return 影响行数
     */
    int deleteById(Connection conn, Object id);

    /**
     * 更新记录
     * @param conn 数据库连接
     * @param entity 实体对象
     * @return 影响行数
     */
    int update(Connection conn, T entity);

    /**
     * 根据 ID 查询单条记录
     * @param conn 数据库连接
     * @param id 主键 ID
     * @return 实体对象，未找到返回 null
     */
    T findById(Connection conn, Object id);

    /**
     * 查询所有记录
     * @param conn 数据库连接
     * @return 实体列表
     */
    List<T> findAll(Connection conn);

    /**
     * 根据条件查询
     * @param conn 数据库连接
     * @param conditions 查询条件（key: 字段名, value: 字段值）
     * @return 实体列表
     */
    List<T> findByCondition(Connection conn, Map<String, Object> conditions);

    /**
     * 分页查询
     * @param conn 数据库连接
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 实体列表
     */
    List<T> findByPage(Connection conn, int pageNum, int pageSize);

    /**
     * 统计记录总数
     * @param conn 数据库连接
     * @return 记录数
     */
    long count(Connection conn);
}
