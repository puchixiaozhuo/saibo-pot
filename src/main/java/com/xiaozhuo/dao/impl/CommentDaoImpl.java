package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.entity.VideoComment;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;
import com.xiaozhuo.util.ShardingUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 评论数据访问实现类（支持水平分表）
 */
public class CommentDaoImpl implements CommentDao {

    private static final Logger logger = LogUtil.getLogger(CommentDaoImpl.class);
    private static final String BASE_TABLE_NAME = "video_comment";

    /**
     * 插入一条评论（自动路由到对应月份的分表）
     */
    @Override
    public int insert(Connection conn, VideoComment comment) {
        // 🔥 根据当前时间计算分表名
        String tableName = ShardingUtil.getCommentTableName(BASE_TABLE_NAME, LocalDateTime.now());

        String sql = "INSERT INTO " + tableName + " (video_id, user_id, parent_id, content, like_count, create_time, update_time) VALUES (?, ?, ?, ?, 0, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, comment.getVideoId());
            ps.setLong(2, comment.getUserId());
            ps.setLong(3, comment.getParentId() != null ? comment.getParentId() : 0);
            ps.setString(4, comment.getContent());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    comment.setId(rs.getLong(1));
                }
            }
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入评论失败: table=" + tableName, e);
            throw new DatabaseException("插入评论失败", e);
        }
    }

    /**
     * 根据ID查询一条评论
     */
    @Override
    public VideoComment selectById(Connection conn, Long id) {
        String sql = "SELECT * FROM video_comment WHERE id = ? AND is_delete = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询评论失败: id=" + id, e);
            throw new DatabaseException("查询评论失败", e);
        }
        return null;
    }

    /**
     * 根据视频ID查询所有评论，按热度排序（聚合最近 6 个月的分表）
     */
    @Override
    public List<VideoComment> selectByVideoIdOrderByHot(Connection conn, Long videoId, int pageNum, int pageSize) {
        return queryCommentsFromShards(conn, videoId, pageNum, pageSize, "like_count DESC, create_time DESC");
    }

    /**
     * 根据视频ID查询所有评论，按时间排序（聚合最近 6 个月的分表）
     */
    @Override
    public List<VideoComment> selectByVideoIdOrderByTime(Connection conn, Long videoId, int pageNum, int pageSize) {
        return queryCommentsFromShards(conn, videoId, pageNum, pageSize, "create_time DESC");
    }

    /**
     * 查询评论列表（通用方法）
     */
    private List<VideoComment> queryComments(Connection conn, String sql, Long videoId, int pageNum, int pageSize) {
        List<VideoComment> comments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, videoId);
            ps.setInt(2, (pageNum - 1) * pageSize);
            ps.setInt(3, pageSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询评论列表失败: videoId=" + videoId, e);
            throw new DatabaseException("查询评论列表失败", e);
        }
        return comments;
    }


    /**
     * 从多个分表中聚合查询评论
     */
    private List<VideoComment> queryCommentsFromShards(Connection conn, Long videoId, int pageNum, int pageSize, String orderBy) {
        String[] tables = ShardingUtil.getRecentMonthTables(BASE_TABLE_NAME, 6);

        StringBuilder sqlBuilder = new StringBuilder();
        for (int i = 0; i < tables.length; i++) {
            if (i > 0) sqlBuilder.append(" UNION ALL ");
            sqlBuilder.append("SELECT * FROM ").append(tables[i])
                    .append(" WHERE video_id = ? AND is_delete = 0");
        }
        sqlBuilder.append(" ORDER BY ").append(orderBy).append(" LIMIT ?, ?");

        List<VideoComment> comments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())) {
            // 为每个子查询设置 video_id
            for (int i = 0; i < tables.length; i++) {
                ps.setLong(i + 1, videoId);
            }
            ps.setInt(tables.length + 1, (pageNum - 1) * pageSize);
            ps.setInt(tables.length + 2, pageSize);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "聚合查询评论失败: videoId=" + videoId, e);
            throw new DatabaseException("查询评论列表失败", e);
        }
        return comments;
    }

    /**
     * 根据视频ID查询评论数量（聚合统计）
     */
    @Override
    public int countByVideoId(Connection conn, Long videoId) {
        String[] tables = ShardingUtil.getRecentMonthTables(BASE_TABLE_NAME, 6);

        StringBuilder sqlBuilder = new StringBuilder("SELECT SUM(cnt) FROM (");
        for (int i = 0; i < tables.length; i++) {
            if (i > 0) sqlBuilder.append(" UNION ALL ");
            sqlBuilder.append("SELECT COUNT(*) as cnt FROM ").append(tables[i])
                    .append(" WHERE video_id = ? AND is_delete = 0");
        }
        sqlBuilder.append(") t");

        try (PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < tables.length; i++) {
                ps.setLong(i + 1, videoId);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计评论数量失败: videoId=" + videoId, e);
        }
        return 0;
    }

    /**
     * 根据评论ID删除一条评论（软删除）
     */
    @Override
    public int deleteById(Connection conn, Long id) {
        String sql = "UPDATE video_comment SET is_delete = 1, update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "删除评论失败: id=" + id, e);
            throw new DatabaseException("删除评论失败", e);
        }
    }

    /**
     * 增加评论的点赞数
     */
    @Override
    public int incrementLikeCount(Connection conn, Long commentId) {
        String sql = "UPDATE video_comment SET like_count = like_count + 1, update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "增加点赞数失败: commentId=" + commentId, e);
            throw new DatabaseException("增加点赞数失败", e);
        }
    }

    /**
     * 减少评论的点赞数
     */
    @Override
    public int decrementLikeCount(Connection conn, Long commentId) {
        String sql = "UPDATE video_comment SET like_count = GREATEST(like_count - 1, 0), update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "减少点赞数失败: commentId=" + commentId, e);
            throw new DatabaseException("减少点赞数失败", e);
        }
    }

    /**
     * 根据用户ID查询所有评论
     */
    @Override
    public List<VideoComment> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_comment WHERE user_id = ? AND is_delete = 0 ORDER BY create_time DESC LIMIT ?, ?";
        List<VideoComment> comments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, (pageNum - 1) * pageSize);
            ps.setInt(3, pageSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询用户评论失败: userId=" + userId, e);
            throw new DatabaseException("查询用户评论失败", e);
        }
        return comments;
    }

    /**
     * 根据评论ID获取作者ID
     */
    @Override
    public Long getAuthorIdById(Connection conn, Long commentId) {
        String sql = "SELECT user_id FROM video_comment WHERE id = ? AND is_delete = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("user_id");
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "获取评论作者ID失败: commentId=" + commentId, e);
            throw new DatabaseException("获取评论作者ID失败", e);
        }
        return null;
    }

    /**
     * 映射行数据
     */
    private VideoComment mapRow(ResultSet rs) throws SQLException {
        VideoComment comment = new VideoComment();
        comment.setId(rs.getLong("id"));
        comment.setVideoId(rs.getLong("video_id"));
        comment.setUserId(rs.getLong("user_id"));
        comment.setParentId(rs.getLong("parent_id"));
        comment.setContent(rs.getString("content"));
        comment.setLikeCount(rs.getLong("like_count"));
        comment.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        comment.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        comment.setIsDelete(rs.getInt("is_delete"));
        return comment;
    }
}
