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
    public Long getAuthorIdById(java.sql.Connection conn, Long videoId) {
        try {
            VideoInfo video = findById(conn, videoId);
            return video != null ? video.getAuthorId() : null;
        } catch (Exception e) {
            LogUtil.logError(logger, "获取作者ID失败: videoId=" + videoId, e);
            throw new DatabaseException("获取作者ID失败", e);
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
