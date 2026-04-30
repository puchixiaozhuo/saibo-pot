package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.FavoriteDao;
import com.xiaozhuo.entity.UserFavorite;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FavoriteDaoImpl implements FavoriteDao {

    private static final Logger logger = LogUtil.getLogger(FavoriteDaoImpl.class);

    @Override
    public int insert(Connection conn, UserFavorite favorite) {
        String sql = "INSERT INTO user_favorite (user_id, video_id, create_time) VALUES (?, ?, ?)";

        LogUtil.logSql(logger, sql, new Object[]{favorite.getUserId(), favorite.getVideoId(), favorite.getCreateTime()});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, favorite.getUserId());
            pstmt.setLong(2, favorite.getVideoId());
            pstmt.setTimestamp(3, Timestamp.valueOf(favorite.getCreateTime()));

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "INSERT_FAVORITE", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "INSERT_FAVORITE failed", e);
            throw new RuntimeException("添加收藏失败", e);
        }
    }

    @Override
    public int deleteByUserIdAndVideoId(Connection conn, Long userId, Long videoId) {
        String sql = "DELETE FROM user_favorite WHERE user_id = ? AND video_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{userId, videoId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, videoId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_FAVORITE", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_FAVORITE failed", e);
            throw new RuntimeException("取消收藏失败", e);
        }
    }

    @Override
    public UserFavorite selectByUserIdAndVideoId(Connection conn, Long userId, Long videoId) {
        String sql = "SELECT * FROM user_favorite WHERE user_id = ? AND video_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{userId, videoId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, videoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserFavorite favorite = new UserFavorite();
                    favorite.setId(rs.getLong("id"));
                    favorite.setUserId(rs.getLong("user_id"));
                    favorite.setVideoId(rs.getLong("video_id"));
                    favorite.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());

                    LogUtil.logBusiness(logger, "SELECT_FAVORITE", "Found favorite record");
                    return favorite;
                }
            }

            LogUtil.logBusiness(logger, "SELECT_FAVORITE", "No favorite record found");
            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_FAVORITE failed", e);
            throw new RuntimeException("查询收藏记录失败", e);
        }
    }

    @Override
    public List<UserFavorite> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM user_favorite WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?";

        LogUtil.logSql(logger, sql, new Object[]{userId, pageSize, offset});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, pageSize);
            pstmt.setInt(3, offset);

            List<UserFavorite> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserFavorite favorite = new UserFavorite();
                    favorite.setId(rs.getLong("id"));
                    favorite.setUserId(rs.getLong("user_id"));
                    favorite.setVideoId(rs.getLong("video_id"));
                    favorite.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    list.add(favorite);
                }
            }

            LogUtil.logBusiness(logger, "SELECT_FAVORITES_BY_USER", "Found " + list.size() + " favorites");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_FAVORITES_BY_USER failed", e);
            throw new RuntimeException("查询收藏列表失败", e);
        }
    }

    @Override
    public long countByUserId(Connection conn, Long userId) {
        String sql = "SELECT COUNT(*) FROM user_favorite WHERE user_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{userId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long count = rs.getLong(1);
                    LogUtil.logBusiness(logger, "COUNT_FAVORITES_BY_USER", "Total " + count + " favorites");
                    return count;
                }
                return 0;
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "COUNT_FAVORITES_BY_USER failed", e);
            throw new RuntimeException("统计收藏数量失败", e);
        }
    }

    @Override
    public long countByVideoId(Connection conn, Long videoId) {
        String sql = "SELECT COUNT(*) FROM user_favorite WHERE video_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{videoId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, videoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long count = rs.getLong(1);
                    LogUtil.logBusiness(logger, "COUNT_FAVORITES_BY_VIDEO", "Total " + count + " favorites");
                    return count;
                }
                return 0;
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "COUNT_FAVORITES_BY_VIDEO failed", e);
            throw new RuntimeException("统计视频收藏数失败", e);
        }
    }
}