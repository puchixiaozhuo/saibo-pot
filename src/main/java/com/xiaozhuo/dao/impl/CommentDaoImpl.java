package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.entity.VideoComment;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 评论数据访问实现类
 */
public class CommentDaoImpl implements CommentDao {

    private static final Logger logger = LogUtil.getLogger(CommentDaoImpl.class);

    /**
     * 插入一条评论
     */
    @Override
    public int insert(Connection conn, VideoComment comment) {
        String sql = "INSERT INTO video_comment (video_id, user_id, parent_id, content, like_count, create_time, update_time) VALUES (?, ?, ?, ?, 0, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, comment.getVideoId());
            ps.setLong(2, comment.getUserId());
            ps.setLong(3, comment.getParentId() != null ? comment.getParentId() : 0);
            ps.setString(4, comment.getContent());

            int rows = ps.executeUpdate();

            // 获取自增ID
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    comment.setId(rs.getLong(1));
                }
            }

            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入评论失败", e);
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
     * 根据视频ID查询所有评论，按热度排序
     */
    @Override
    public List<VideoComment> selectByVideoIdOrderByHot(Connection conn, Long videoId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_comment WHERE video_id = ? AND is_delete = 0 ORDER BY like_count DESC, create_time DESC LIMIT ?, ?";
        return queryComments(conn, sql, videoId, pageNum, pageSize);
    }

    /**
     * 根据视频ID查询所有评论，按时间排序
     */
    @Override
    public List<VideoComment> selectByVideoIdOrderByTime(Connection conn, Long videoId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_comment WHERE video_id = ? AND is_delete = 0 ORDER BY create_time DESC LIMIT ?, ?";
        return queryComments(conn, sql, videoId, pageNum, pageSize);
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
     * 根据视频ID查询评论数量
     */
    @Override
    public int countByVideoId(Connection conn, Long videoId) {
        String sql = "SELECT COUNT(*) FROM video_comment WHERE video_id = ? AND is_delete = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, videoId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计评论数量失败: videoId=" + videoId, e);
            throw new DatabaseException("统计评论数量失败", e);
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
     * 将 ResultSet 映射为 VideoComment 对象
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
