package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.entity.VideoComment;
import com.xiaozhuo.util.JDBCUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDaoImpl implements CommentDao {

    @Override
    public int insert(Connection conn, VideoComment comment) {
        String sql = "INSERT INTO video_comment (video_id, user_id, parent_id, content, like_count, create_time, update_time) VALUES (?, ?, ?, ?, 0, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, comment.getVideoId());
            ps.setLong(2, comment.getUserId());
            ps.setLong(3, comment.getParentId() != null ? comment.getParentId() : 0);
            ps.setString(4, comment.getContent());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

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
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<VideoComment> selectByVideoIdOrderByHot(Connection conn, Long videoId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_comment WHERE video_id = ? AND is_delete = 0 ORDER BY like_count DESC, create_time DESC LIMIT ?, ?";
        return queryComments(conn, sql, videoId, pageNum, pageSize);
    }

    @Override
    public List<VideoComment> selectByVideoIdOrderByTime(Connection conn, Long videoId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_comment WHERE video_id = ? AND is_delete = 0 ORDER BY create_time DESC LIMIT ?, ?";
        return queryComments(conn, sql, videoId, pageNum, pageSize);
    }


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
            e.printStackTrace();
        }
        return comments;
    }

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
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int deleteById(Connection conn, Long id) {
        String sql = "UPDATE video_comment SET is_delete = 1, update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int incrementLikeCount(Connection conn, Long commentId) {
        String sql = "UPDATE video_comment SET like_count = like_count + 1, update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int decrementLikeCount(Connection conn, Long commentId) {
        String sql = "UPDATE video_comment SET like_count = GREATEST(like_count - 1, 0), update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, commentId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

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
            e.printStackTrace();
        }
        return comments;
    }

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
            e.printStackTrace();
        }
        return null;
    }

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

