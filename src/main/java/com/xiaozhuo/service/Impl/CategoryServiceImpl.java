package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.dao.CategoryDao;
import com.xiaozhuo.dao.impl.CategoryDaoImpl;
import com.xiaozhuo.entity.VideoCategory;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CategoryService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Bean
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LogUtil.getLogger(CategoryServiceImpl.class);

    private CategoryDao categoryDao = new CategoryDaoImpl();

    @Override
    @Transactional
    public Result<Long> createCategory(VideoCategory category) {
        Connection conn = null;
        try {
            if (category == null || category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                return Result.fail(400, "分类名称不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoCategory existingCategory = categoryDao.selectByName(conn, category.getCategoryName());
            if (existingCategory != null) {
                return Result.fail(400, "分类名称已存在");
            }

            if (category.getParentId() == null) {
                category.setParentId(0L);
            }
            if (category.getSortOrder() == null) {
                category.setSortOrder(0);
            }
            if (category.getIsDelete() == null) {
                category.setIsDelete(0);
            }
            category.setCreateTime(LocalDateTime.now());
            category.setUpdateTime(LocalDateTime.now());

            int rows = categoryDao.insert(conn, category);
            if (rows <= 0) {
                return Result.fail(500, "创建分类失败");
            }

            System.out.println("✅ 分类创建成功 - CategoryID: " + category.getId()
                    + ", Name: " + category.getCategoryName());

            return Result.success("分类创建成功", category.getId());

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "创建分类失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> updateCategory(VideoCategory category) {
        Connection conn = null;
        try {
            if (category == null || category.getId() == null) {
                return Result.fail(400, "分类ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoCategory existingCategory = categoryDao.selectById(conn, category.getId());
            if (existingCategory == null) {
                return Result.fail(404, "分类不存在");
            }

            if (category.getCategoryName() != null) {
                VideoCategory duplicateCategory = categoryDao.selectByName(conn, category.getCategoryName());
                if (duplicateCategory != null && !duplicateCategory.getId().equals(category.getId())) {
                    return Result.fail(400, "分类名称已存在");
                }
                existingCategory.setCategoryName(category.getCategoryName());
            }

            if (category.getParentId() != null) {
                existingCategory.setParentId(category.getParentId());
            }
            if (category.getSortOrder() != null) {
                existingCategory.setSortOrder(category.getSortOrder());
            }
            existingCategory.setUpdateTime(LocalDateTime.now());

            int rows = categoryDao.update(conn, existingCategory);
            if (rows <= 0) {
                return Result.fail(500, "更新分类失败");
            }

            System.out.println("✅ 分类更新成功 - CategoryID: " + category.getId());

            return Result.success("分类更新成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "更新分类失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> deleteCategory(Long categoryId) {
        Connection conn = null;
        try {
            if (categoryId == null) {
                return Result.fail(400, "分类ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoCategory category = categoryDao.selectById(conn, categoryId);
            if (category == null) {
                return Result.fail(404, "分类不存在");
            }

            List<VideoCategory> subCategories = categoryDao.selectByParentId(conn, categoryId);
            if (!subCategories.isEmpty()) {
                return Result.fail(400, "该分类下还有子分类，请先删除子分类");
            }

            int rows = categoryDao.delete(conn, categoryId);
            if (rows <= 0) {
                return Result.fail(500, "删除分类失败");
            }

            System.out.println("✅ 分类删除成功 - CategoryID: " + categoryId);

            return Result.success("分类删除成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "删除分类失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<VideoCategory>> getAllCategories() {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            List<VideoCategory> categories = categoryDao.selectAll(conn);

            return Result.success("查询成功", categories);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询分类列表失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<VideoCategory>> getSubCategories(Long parentId) {
        Connection conn = null;
        try {
            if (parentId == null) {
                return Result.fail(400, "父分类ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            List<VideoCategory> categories = categoryDao.selectByParentId(conn, parentId);

            return Result.success("查询成功", categories);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询子分类失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<VideoCategory> getCategoryById(Long categoryId) {
        Connection conn = null;
        try {
            if (categoryId == null) {
                return Result.fail(400, "分类ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoCategory category = categoryDao.selectById(conn, categoryId);
            if (category == null) {
                return Result.fail(404, "分类不存在");
            }

            return Result.success("查询成功", category);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询分类详情失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }
}