package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.util.JDBCUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VideoDaoImpl implements VideoDao {

    @Override
    public int insert(VideoInfo video) {
        String sql = "INSERT INTO video_info(author_id, title, cover, video_url, duration, file_size, format, resolution, cache_status, transcode_status, category_id, description) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, video.getAuthorId());
            pstmt.setString(2, video.getTitle());
            pstmt.setString(3, video.getCover());
            pstmt.setString(4, video.getVideoUrl());
            pstmt.setInt(5, video.getDuration() != null ? video.getDuration() : 0);
            pstmt.setLong(6, video.getFileSize() != null ? video.getFileSize() : 0);
            pstmt.setString(7, video.getFormat() != null ? video.getFormat() : "mp4");
            pstmt.setString(8, video.getResolution() != null ? video.getResolution() : "1080p");
            pstmt.setInt(9, video.getCacheStatus() != null ? video.getCacheStatus() : 0);
            pstmt.setInt(10, video.getTranscodeStatus() != null ? video.getTranscodeStatus() : 0);
            pstmt.setLong(11, video.getCategoryId());
            pstmt.setString(12, video.getDescription());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    video.setId(rs.getLong(1));
                }
            }
            return rows;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            JDBCUtil.close(conn, pstmt);
        }
    }

    @Override
    public int update(VideoInfo video) {
        String sql = "UPDATE video_info SET title=?, cover=?, video_url=?, duration=?, file_size=?, format=?, resolution=?, cache_status=?, transcode_status=?, category_id=?, description=? WHERE id=?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, video.getTitle());
            pstmt.setString(2, video.getCover());
            pstmt.setString(3, video.getVideoUrl());
            pstmt.setInt(4, video.getDuration());
            pstmt.setLong(5, video.getFileSize());
            pstmt.setString(6, video.getFormat());
            pstmt.setString(7, video.getResolution());
            pstmt.setInt(8, video.getCacheStatus());
            pstmt.setInt(9, video.getTranscodeStatus());
            pstmt.setLong(10, video.getCategoryId());
            pstmt.setString(11, video.getDescription());
            pstmt.setLong(12, video.getId());
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            JDBCUtil.close(conn, pstmt);
        }
    }

    @Override
    public int deleteById(Long id) {
        String sql = "UPDATE video_info SET is_delete=1 WHERE id=?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, id);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            JDBCUtil.close(conn, pstmt);
        }
    }

    @Override
    public VideoInfo selectById(Long id) {
        String sql = "SELECT * FROM video_info WHERE id=? AND is_delete=0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToVideo(rs);
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            JDBCUtil.close(conn, pstmt, rs);
        }
    }

    @Override
    public List<VideoInfo> selectAll(int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_info WHERE is_delete=0 ORDER BY create_time DESC LIMIT ?,?";
        return queryVideos(sql, (pageNum - 1) * pageSize, pageSize);
    }

    @Override
    public List<VideoInfo> selectByAuthorId(Long authorId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_info WHERE author_id=? AND is_delete=0 ORDER BY create_time DESC LIMIT ?,?";
        return queryVideos(sql, authorId, (pageNum - 1) * pageSize, pageSize);
    }

    @Override
    public List<VideoInfo> selectByCategoryId(Long categoryId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_info WHERE category_id=? AND is_delete=0 ORDER BY create_time DESC LIMIT ?,?";
        return queryVideos(sql, categoryId, (pageNum - 1) * pageSize, pageSize);
    }

    @Override
    public List<VideoInfo> searchByTitle(String keyword, int pageNum, int pageSize) {
        String sql = "SELECT * FROM video_info WHERE title LIKE ? AND is_delete=0 ORDER BY create_time DESC LIMIT ?,?";
        return queryVideos(sql, "%" + keyword + "%", (pageNum - 1) * pageSize, pageSize);
    }

    @Override
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM video_info WHERE is_delete=0";
        return count(sql);
    }

    @Override
    public int countByAuthorId(Long authorId) {
        String sql = "SELECT COUNT(*) FROM video_info WHERE author_id=? AND is_delete=0";
        return count(sql, authorId);
    }

    @Override
    public int countByCategoryId(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM video_info WHERE category_id=? AND is_delete=0";
        return count(sql, categoryId);
    }

    @Override
    public int incrementViewCount(Long id) {
        String sql = "UPDATE video_info SET view_count=view_count+1 WHERE id=?";
        return executeUpdate(sql, id);
    }

    @Override
    public int decrementViewCount(Long id) {
        String sql = "UPDATE video_info SET view_count=GREATEST(view_count-1,0) WHERE id=?";
        return executeUpdate(sql, id);
    }
    @Override
    public int incrementLikeCount(Long id) {
        String sql = "UPDATE video_info SET like_count=like_count+1 WHERE id=?";
        return executeUpdate(sql, id);
    }

    @Override
    public int decrementLikeCount(Long id) {
        String sql = "UPDATE video_info SET like_count=GREATEST(like_count-1,0) WHERE id=?";
        return executeUpdate(sql, id);
    }

    @Override
    public int incrementCommentCount(Long id) {
        String sql = "UPDATE video_info SET comment_count=comment_count+1 WHERE id=?";
        return executeUpdate(sql, id);
    }

    @Override
    public int decrementCommentCount(Long id) {
        String sql = "UPDATE video_info SET comment_count=GREATEST(comment_count-1,0) WHERE id=?";
        return executeUpdate(sql, id);
    }

    @Override
    public Long getAuthorIdById(Connection conn, Long videoId) {
        String sql = "SELECT author_id FROM video_info WHERE id = ? AND is_delete = 0";
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            if (conn == null) {
                conn = JDBCUtil.getConnection();
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, videoId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getLong("author_id");
            }

            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private VideoInfo mapRowToVideo(ResultSet rs) throws SQLException {
        VideoInfo video = new VideoInfo();
        video.setId(rs.getLong("id"));
        video.setAuthorId(rs.getLong("author_id"));
        video.setTitle(rs.getString("title"));
        video.setCover(rs.getString("cover"));
        video.setVideoUrl(rs.getString("video_url"));
        video.setDuration(rs.getInt("duration"));
        video.setFileSize(rs.getLong("file_size"));
        video.setFormat(rs.getString("format"));
        video.setResolution(rs.getString("resolution"));
        video.setCacheStatus(rs.getInt("cache_status"));
        video.setTranscodeStatus(rs.getInt("transcode_status"));
        video.setCategoryId(rs.getLong("category_id"));
        video.setDescription(rs.getString("description"));
        video.setViewCount(rs.getLong("view_count"));
        video.setLikeCount(rs.getLong("like_count"));
        video.setCommentCount(rs.getLong("comment_count"));
        video.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        video.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        video.setIsDelete(rs.getInt("is_delete"));
        return video;
    }

    private List<VideoInfo> queryVideos(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<VideoInfo> videos = new ArrayList<>();
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            rs = pstmt.executeQuery();
            while (rs.next()) {
                videos.add(mapRowToVideo(rs));
            }
            return videos;
        } catch (SQLException e) {
            e.printStackTrace();
            return videos;
        } finally {
            JDBCUtil.close(conn, pstmt, rs);
        }
    }

    private int count(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            JDBCUtil.close(conn, pstmt, rs);
        }
    }

    private int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            JDBCUtil.close(conn, pstmt);
        }
    }
}
