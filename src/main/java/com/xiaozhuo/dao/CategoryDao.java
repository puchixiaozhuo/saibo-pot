package com.xiaozhuo.dao;

import com.xiaozhuo.entity.VideoCategory;

import java.sql.Connection;
import java.util.List;

public interface CategoryDao {
    /**
     * 插入分类
     * @param conn 数据库连接
     * @param category 分类信息
     * @return 影响行数
     */
    int insert(Connection conn, VideoCategory category);

    /**
     * 更新分类
     * @param conn 数据库连接
     * @param category 分类信息
     * @return 影响行数
     */
    int update(Connection conn, VideoCategory category);

    /**
     * 删除分类（逻辑删除）
     * @param conn 数据库连接
     * @param categoryId 分类ID
     * @return 影响行数
     */
    int delete(Connection conn, Long categoryId);

    /**
     * 根据ID查询分类
     * @param conn 数据库连接
     * @param categoryId 分类ID
     * @return 分类信息
     */
    VideoCategory selectById(Connection conn, Long categoryId);

    /**
     * 查询所有分类（按排序权重）
     * @param conn 数据库连接
     * @return 分类列表
     */
    List<VideoCategory> selectAll(Connection conn);

    /**
     * 根据父分类ID查询子分类
     * @param conn 数据库连接
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<VideoCategory> selectByParentId(Connection conn, Long parentId);

    /**
     * 根据分类名称查询
     * @param conn 数据库连接
     * @param categoryName 分类名称
     * @return 分类信息
     */
    VideoCategory selectByName(Connection conn, String categoryName);
}