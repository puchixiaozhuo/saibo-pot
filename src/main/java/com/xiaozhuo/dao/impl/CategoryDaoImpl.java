package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.CategoryDao;
import com.xiaozhuo.entity.VideoCategory;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CategoryDaoImpl implements CategoryDao {

    private static final Logger logger = LogUtil.getLogger(CategoryDaoImpl.class);

    @Override
    public int insert(Connection conn, VideoCategory category) {
        String sql = "INSERT INTO video_category (category_name, parent_id, sort_order, is_delete, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?)";

        LogUtil.logSql(logger, sql, new Object[]{
            category.getCategoryName(), category.getParentId(),
            category.getSortOrder(), category.getIsDelete(),
            category.getCreateTime(), category.getUpdateTime()
        });

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setLong(2, category.getParentId() != null ? category.getParentId() : 0);
            pstmt.setInt(3, category.getSortOrder() != null ? category.getSortOrder() : 0);
            pstmt.setInt(4, category.getIsDelete() != null ? category.getIsDelete() : 0);
            pstmt.setTimestamp(5, Timestamp.valueOf(category.getCreateTime()));
            pstmt.setTimestamp(6, Timestamp.valueOf(category.getUpdateTime()));

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    category.setId(rs.getLong(1));
                }
            }

            LogUtil.logBusiness(logger, "INSERT_CATEGORY", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "INSERT_CATEGORY failed", e);
            throw new RuntimeException("添加分类失败", e);
        }
    }

    @Override
    public int update(Connection conn, VideoCategory category) {
        String sql = "UPDATE video_category SET category_name = ?, parent_id = ?, sort_order = ?, update_time = ? WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{
            category.getCategoryName(), category.getParentId(),
            category.getSortOrder(), category.getUpdateTime(), category.getId()
        });

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setLong(2, category.getParentId() != null ? category.getParentId() : 0);
            pstmt.setInt(3, category.getSortOrder() != null ? category.getSortOrder() : 0);
            pstmt.setTimestamp(4, Timestamp.valueOf(category.getUpdateTime()));
            pstmt.setLong(5, category.getId());

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "UPDATE_CATEGORY", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "UPDATE_CATEGORY failed", e);
            throw new RuntimeException("更新分类失败", e);
        }
    }

    @Override
    public int delete(Connection conn, Long categoryId) {
        String sql = "UPDATE video_category SET is_delete = 1, update_time = ? WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{categoryId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.setLong(2, categoryId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_CATEGORY", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_CATEGORY failed", e);
            throw new RuntimeException("删除分类失败", e);
        }
    }

    @Override
    public VideoCategory selectById(Connection conn, Long categoryId) {
        String sql = "SELECT * FROM video_category WHERE id = ? AND is_delete = 0";

        LogUtil.logSql(logger, sql, new Object[]{categoryId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    VideoCategory category = new VideoCategory();
                    category.setId(rs.getLong("id"));
                    category.setCategoryName(rs.getString("category_name"));
                    category.setParentId(rs.getLong("parent_id"));
                    category.setSortOrder(rs.getInt("sort_order"));
                    category.setIsDelete(rs.getInt("is_delete"));
                    category.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    category.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());

                    LogUtil.logBusiness(logger, "SELECT_CATEGORY_BY_ID", "Found category");
                    return category;
                }
            }

            LogUtil.logBusiness(logger, "SELECT_CATEGORY_BY_ID", "No category found");
            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_CATEGORY_BY_ID failed", e);
            throw new RuntimeException("查询分类失败", e);
        }
    }

    @Override
    public List<VideoCategory> selectAll(Connection conn) {
        String sql = "SELECT * FROM video_category WHERE is_delete = 0 ORDER BY sort_order ASC, create_time DESC";

        LogUtil.logSql(logger, sql, new Object[]{});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            List<VideoCategory> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VideoCategory category = new VideoCategory();
                    category.setId(rs.getLong("id"));
                    category.setCategoryName(rs.getString("category_name"));
                    category.setParentId(rs.getLong("parent_id"));
                    category.setSortOrder(rs.getInt("sort_order"));
                    category.setIsDelete(rs.getInt("is_delete"));
                    category.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    category.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                    list.add(category);
                }
            }

            LogUtil.logBusiness(logger, "SELECT_ALL_CATEGORIES", "Found " + list.size() + " categories");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_ALL_CATEGORIES failed", e);
            throw new RuntimeException("查询分类列表失败", e);
        }
    }

    @Override
    public List<VideoCategory> selectByParentId(Connection conn, Long parentId) {
        String sql = "SELECT * FROM video_category WHERE parent_id = ? AND is_delete = 0 ORDER BY sort_order ASC";

        LogUtil.logSql(logger, sql, new Object[]{parentId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, parentId);

            List<VideoCategory> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VideoCategory category = new VideoCategory();
                    category.setId(rs.getLong("id"));
                    category.setCategoryName(rs.getString("category_name"));
                    category.setParentId(rs.getLong("parent_id"));
                    category.setSortOrder(rs.getInt("sort_order"));
                    category.setIsDelete(rs.getInt("is_delete"));
                    category.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    category.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                    list.add(category);
                }
            }

            LogUtil.logBusiness(logger, "SELECT_CATEGORIES_BY_PARENT", "Found " + list.size() + " categories");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_CATEGORIES_BY_PARENT failed", e);
            throw new RuntimeException("查询子分类失败", e);
        }
    }

    @Override
    public VideoCategory selectByName(Connection conn, String categoryName) {
        String sql = "SELECT * FROM video_category WHERE category_name = ? AND is_delete = 0";

        LogUtil.logSql(logger, sql, new Object[]{categoryName});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    VideoCategory category = new VideoCategory();
                    category.setId(rs.getLong("id"));
                    category.setCategoryName(rs.getString("category_name"));
                    category.setParentId(rs.getLong("parent_id"));
                    category.setSortOrder(rs.getInt("sort_order"));
                    category.setIsDelete(rs.getInt("is_delete"));
                    category.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    category.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());

                    LogUtil.logBusiness(logger, "SELECT_CATEGORY_BY_NAME", "Found category");
                    return category;
                }
            }

            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_CATEGORY_BY_NAME failed", e);
            throw new RuntimeException("查询分类失败", e);
        }
    }
}
