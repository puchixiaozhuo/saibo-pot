package com.xiaozhuo.service;

import com.xiaozhuo.entity.VideoCategory;
import com.xiaozhuo.result.Result;

import java.util.List;

public interface CategoryService {

    /**
     * 创建分类
     * @param category 分类信息
     * @return 创建结果
     */
    Result<Long> createCategory(VideoCategory category);

    /**
     * 更新分类
     * @param category 分类信息
     * @return 更新结果
     */
    Result<Boolean> updateCategory(VideoCategory category);

    /**
     * 删除分类
     * @param categoryId 分类ID
     * @return 删除结果
     */
    Result<Boolean> deleteCategory(Long categoryId);

    /**
     * 查询所有分类
     * @return 分类列表
     */
    Result<List<VideoCategory>> getAllCategories();

    /**
     * 根据父分类ID查询子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    Result<List<VideoCategory>> getSubCategories(Long parentId);

    /**
     * 根据ID查询分类详情
     * @param categoryId 分类ID
     * @return 分类信息
     */
    Result<VideoCategory> getCategoryById(Long categoryId);
}