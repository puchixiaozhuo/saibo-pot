package com.xiaozhuo.dao.impl;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * VideoDao实现类 - 继承BaseDaoImpl并扩展特定方法
 */
public class VideoDaoImpl extends BaseDaoImpl<VideoInfo> implements VideoDao {

    private static final Logger logger = LogUtil.getLogger(VideoDaoImpl.class);

    public VideoDaoImpl() {
        super(VideoInfo.class);
    }

    @Override
    public int insert(VideoInfo video) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            return super.insert(conn, video);
        } catch (Exception e) {
            LogUtil.logError(logger, "插入视频失败", e);
            throw new DatabaseException("插入视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int update(VideoInfo video) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            return super.update(conn, video);
        } catch (Exception e) {
            LogUtil.logError(logger, "更新视频失败", e);
            throw new DatabaseException("更新视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int deleteById(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            return super.deleteById(conn, id);
        } catch (Exception e) {
            LogUtil.logError(logger, "删除视频失败: id=" + id, e);
            throw new DatabaseException("删除视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public VideoInfo selectById(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            return super.findById(conn, id);
        } catch (Exception e) {
            LogUtil.logError(logger, "查询视频失败: id=" + id, e);
            throw new DatabaseException("查询视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public List<VideoInfo> selectAll(int pageNum, int pageSize) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            return findByPage(conn, pageNum, pageSize);
        } catch (Exception e) {
            LogUtil.logError(logger, "查询视频列表失败", e);
            throw new DatabaseException("查询视频列表失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public List<VideoInfo> selectByAuthorId(Long authorId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("author_id", authorId);
            conditions.put("is_delete", 0);
            return findByCondition(conn, conditions);
        } catch (Exception e) {
            LogUtil.logError(logger, "根据作者ID查询视频失败: authorId=" + authorId, e);
            throw new DatabaseException("根据作者ID查询视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public List<VideoInfo> selectByCategoryId(Long categoryId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("category_id", categoryId);
            conditions.put("is_delete", 0);
            return findByCondition(conn, conditions);
        } catch (Exception e) {
            LogUtil.logError(logger, "根据分类ID查询视频失败: categoryId=" + categoryId, e);
            throw new DatabaseException("根据分类ID查询视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public List<VideoInfo> searchByTitle(String keyword, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            String sql = "SELECT * FROM video_info WHERE title LIKE ? AND is_delete = 0 ORDER BY create_time DESC LIMIT ? OFFSET ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + keyword + "%");
                pstmt.setInt(2, pageSize);
                pstmt.setInt(3, (pageNum - 1) * pageSize);

                try (ResultSet rs = pstmt.executeQuery()) {
                    return mapResultSetToList(rs);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "搜索视频失败: keyword=" + keyword, e);
            throw new DatabaseException("搜索视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int countAll() {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            return (int) super.count(conn);
        } catch (Exception e) {
            LogUtil.logError(logger, "统计视频数量失败", e);
            throw new DatabaseException("统计视频数量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int countByAuthorId(Long authorId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("author_id", authorId);
            conditions.put("is_delete", 0);
            return findByCondition(conn, conditions).size();
        } catch (Exception e) {
            LogUtil.logError(logger, "统计作者视频数量失败: authorId=" + authorId, e);
            throw new DatabaseException("统计作者视频数量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int countByCategoryId(Long categoryId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("category_id", categoryId);
            conditions.put("is_delete", 0);
            return findByCondition(conn, conditions).size();
        } catch (Exception e) {
            LogUtil.logError(logger, "统计分类视频数量失败: categoryId=" + categoryId, e);
            throw new DatabaseException("统计分类视频数量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int incrementViewCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null) {
                video.setViewCount(video.getViewCount() + 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "增加播放量失败: id=" + id, e);
            throw new DatabaseException("增加播放量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int decrementViewCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null && video.getViewCount() > 0) {
                video.setViewCount(video.getViewCount() - 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "减少播放量失败: id=" + id, e);
            throw new DatabaseException("减少播放量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int incrementLikeCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null) {
                video.setLikeCount(video.getLikeCount() + 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "增加点赞数失败: id=" + id, e);
            throw new DatabaseException("增加点赞数失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int decrementLikeCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null && video.getLikeCount() > 0) {
                video.setLikeCount(video.getLikeCount() - 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "减少点赞数失败: id=" + id, e);
            throw new DatabaseException("减少点赞数失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int incrementCommentCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null) {
                video.setCommentCount(video.getCommentCount() + 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "增加评论数失败: id=" + id, e);
            throw new DatabaseException("增加评论数失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int decrementCommentCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null && video.getCommentCount() > 0) {
                video.setCommentCount(video.getCommentCount() - 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "减少评论数失败: id=" + id, e);
            throw new DatabaseException("减少评论数失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }


    @Override
    public int incrementFavoriteCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null) {
                Long currentCount = video.getFavoriteCount() != null ? video.getFavoriteCount() : 0L;
                video.setFavoriteCount(currentCount + 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "增加收藏数失败: id=" + id, e);
            throw new DatabaseException("增加收藏数失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public int decrementFavoriteCount(Long id) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            VideoInfo video = findById(conn, id);
            if (video != null && video.getFavoriteCount() != null && video.getFavoriteCount() > 0) {
                video.setFavoriteCount(video.getFavoriteCount() - 1);
                return update(conn, video);
            }
            return 0;
        } catch (Exception e) {
            LogUtil.logError(logger, "减少收藏数失败: id=" + id, e);
            throw new DatabaseException("减少收藏数失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Long getAuthorIdById(java.sql.Connection conn, Long videoId) {
        try {
            VideoInfo video = findById(conn, videoId);
            return video != null ? video.getAuthorId() : null;
        } catch (Exception e) {
            LogUtil.logError(logger, "获取作者ID失败: videoId=" + videoId, e);
            throw new DatabaseException("获取作者ID失败", e);
        }
    }


    @Override
    public List<VideoInfo> selectVideosByAuthorIds(List<Long> authorIds, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            if (authorIds == null || authorIds.isEmpty()) {
                return new ArrayList<>();
            }

            String placeholders = String.join(",", authorIds.stream().map(id -> "?").toArray(String[]::new));
            String sql = "SELECT * FROM video_info WHERE author_id IN (" + placeholders + ") AND is_delete = 0 ORDER BY create_time DESC LIMIT ? OFFSET ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < authorIds.size(); i++) {
                    pstmt.setLong(i + 1, authorIds.get(i));
                }
                pstmt.setInt(authorIds.size() + 1, pageSize);
                pstmt.setInt(authorIds.size() + 2, (pageNum - 1) * pageSize);

                try (ResultSet rs = pstmt.executeQuery()) {
                    return mapResultSetToList(rs);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "根据作者ID列表查询视频失败", e);
            throw new DatabaseException("根据作者ID列表查询视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public long countVideosByAuthorIds(List<Long> authorIds) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            if (authorIds == null || authorIds.isEmpty()) {
                return 0;
            }

            String placeholders = String.join(",", authorIds.stream().map(id -> "?").toArray(String[]::new));
            String sql = "SELECT COUNT(*) FROM video_info WHERE author_id IN (" + placeholders + ") AND is_delete = 0";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < authorIds.size(); i++) {
                    pstmt.setLong(i + 1, authorIds.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    return 0;
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计作者视频数量失败", e);
            throw new DatabaseException("统计作者视频数量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public List<VideoInfo> selectVideosByAuthorIdsWithCursor(List<Long> authorIds, String cursor, int pageSize) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            if (authorIds == null || authorIds.isEmpty()) {
                return new ArrayList<>();
            }

            String placeholders = String.join(",", authorIds.stream().map(id -> "?").toArray(String[]::new));
            String sql;

            if (cursor == null || cursor.isEmpty()) {
                sql = "SELECT * FROM video_info WHERE author_id IN (" + placeholders + ") AND is_delete = 0 ORDER BY create_time DESC LIMIT ?";
            } else {
                sql = "SELECT * FROM video_info WHERE author_id IN (" + placeholders + ") AND is_delete = 0 AND id < ? ORDER BY create_time DESC LIMIT ?";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < authorIds.size(); i++) {
                    pstmt.setLong(i + 1, authorIds.get(i));
                }

                if (cursor != null && !cursor.isEmpty()) {
                    pstmt.setLong(authorIds.size() + 1, Long.parseLong(cursor));
                    pstmt.setInt(authorIds.size() + 2, pageSize);
                } else {
                    pstmt.setInt(authorIds.size() + 1, pageSize);
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    return mapResultSetToList(rs);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "基于游标查询视频失败", e);
            throw new DatabaseException("基于游标查询视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public long countUnreadVideos(List<Long> authorIds, java.time.LocalDateTime lastReadTime) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            if (authorIds == null || authorIds.isEmpty() || lastReadTime == null) {
                return 0;
            }

            String placeholders = String.join(",", authorIds.stream().map(id -> "?").toArray(String[]::new));
            String sql = "SELECT COUNT(*) FROM video_info WHERE author_id IN (" + placeholders + ") AND is_delete = 0 AND create_time > ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < authorIds.size(); i++) {
                    pstmt.setLong(i + 1, authorIds.get(i));
                }
                pstmt.setTimestamp(authorIds.size() + 1, java.sql.Timestamp.valueOf(lastReadTime));

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    return 0;
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计未读视频数量失败", e);
            throw new DatabaseException("统计未读视频数量失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }


    @Override
    public List<VideoInfo> selectVideosByIds(List<Long> videoIds) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            if (videoIds == null || videoIds.isEmpty()) {
                return new ArrayList<>();
            }

            StringBuilder sql = new StringBuilder("SELECT * FROM video_info WHERE id IN (");
            for (int i = 0; i < videoIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(") AND is_delete = 0 ORDER BY create_time DESC");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < videoIds.size(); i++) {
                    pstmt.setLong(i + 1, videoIds.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    return mapResultSetToList(rs);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "根据ID列表查询视频失败", e);
            throw new DatabaseException("根据ID列表查询视频失败", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }



    /**
     * 将 ResultSet 映射为实体列表
     */
    private List<VideoInfo> mapResultSetToList(ResultSet rs) throws SQLException {
        List<VideoInfo> list = new java.util.ArrayList<>();
        while (rs.next()) {
            try {
                list.add(mapResultSetToEntity(rs));
            } catch (Exception e) {
                LogUtil.logError(logger, "映射单条记录失败", e);
            }
        }
        return list;
    }


    /**
     * 将 ResultSet 映射为单个实体
     */
    private VideoInfo mapResultSetToEntity(ResultSet rs) throws SQLException {
        try {
            VideoInfo video = new VideoInfo();
            video.setId(rs.getLong("id"));
            video.setAuthorId(rs.getLong("author_id"));
            video.setTitle(rs.getString("title"));
            video.setDescription(rs.getString("description"));
            video.setVideoUrl(rs.getString("video_url"));
            video.setCover(rs.getString("cover"));
            video.setCategoryId(rs.getLong("category_id"));
            video.setResolution(rs.getString("resolution"));
            video.setFileSize(rs.getLong("file_size"));
            video.setFormat(rs.getString("format"));
            video.setDuration(rs.getInt("duration"));
            video.setViewCount(rs.getLong("view_count"));
            video.setLikeCount(rs.getLong("like_count"));
            video.setCommentCount(rs.getLong("comment_count"));
            video.setCacheStatus(rs.getInt("cache_status"));
            video.setTranscodeStatus(rs.getInt("transcode_status"));
            video.setIsDelete(rs.getInt("is_delete"));
            video.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            video.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            return video;
        } catch (SQLException e) {
            LogUtil.logError(logger, "映射ResultSet失败", e);
            throw e;
        }
    }
}
