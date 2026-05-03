package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.WatchHistoryDao;
import com.xiaozhuo.entity.UserWatchHistory;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WatchHistoryDaoImpl implements WatchHistoryDao {

    private static final Logger logger = LogUtil.getLogger(WatchHistoryDaoImpl.class);

    @Override
    public int upsert(Connection conn, UserWatchHistory history) {
        String sql = "INSERT INTO user_watch_history (user_id, video_id, watch_progress, watch_duration, watch_time) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "watch_progress = VALUES(watch_progress), " +
                     "watch_duration = VALUES(watch_duration), " +
                     "watch_time = VALUES(watch_time)";

        LogUtil.logSql(logger, sql, new Object[]{
            history.getUserId(), history.getVideoId(),
            history.getWatchProgress(), history.getWatchDuration(),
            history.getWatchTime()
        });

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, history.getUserId());
            pstmt.setLong(2, history.getVideoId());
            pstmt.setInt(3, history.getWatchProgress() != null ? history.getWatchProgress() : 0);
            pstmt.setInt(4, history.getWatchDuration() != null ? history.getWatchDuration() : 0);
            pstmt.setTimestamp(5, Timestamp.valueOf(history.getWatchTime()));

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "UPSERT_WATCH_HISTORY", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "UPSERT_WATCH_HISTORY failed", e);
            throw new RuntimeException("保存观看记录失败", e);
        }
    }

    @Override
    public List<UserWatchHistory> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM user_watch_history WHERE user_id = ? ORDER BY watch_time DESC LIMIT ? OFFSET ?";

        LogUtil.logSql(logger, sql, new Object[]{userId, pageSize, offset});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, pageSize);
            pstmt.setInt(3, offset);

            List<UserWatchHistory> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserWatchHistory history = new UserWatchHistory();
                    history.setId(rs.getLong("id"));
                    history.setUserId(rs.getLong("user_id"));
                    history.setVideoId(rs.getLong("video_id"));
                    history.setWatchProgress(rs.getInt("watch_progress"));
                    history.setWatchDuration(rs.getInt("watch_duration"));
                    history.setWatchTime(rs.getTimestamp("watch_time").toLocalDateTime());
                    list.add(history);
                }
            }

            LogUtil.logBusiness(logger, "SELECT_WATCH_HISTORY_BY_USER", "Found " + list.size() + " records");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_WATCH_HISTORY_BY_USER failed", e);
            throw new RuntimeException("查询观看历史失败", e);
        }
    }

    @Override
    public long countByUserId(Connection conn, Long userId) {
        String sql = "SELECT COUNT(*) FROM user_watch_history WHERE user_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{userId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long count = rs.getLong(1);
                    LogUtil.logBusiness(logger, "COUNT_WATCH_HISTORY", "Total " + count + " records");
                    return count;
                }
                return 0;
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "COUNT_WATCH_HISTORY failed", e);
            throw new RuntimeException("统计观看历史数量失败", e);
        }
    }

    @Override
    public UserWatchHistory selectByUserIdAndVideoId(Connection conn, Long userId, Long videoId) {
        String sql = "SELECT * FROM user_watch_history WHERE user_id = ? AND video_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{userId, videoId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, videoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserWatchHistory history = new UserWatchHistory();
                    history.setId(rs.getLong("id"));
                    history.setUserId(rs.getLong("user_id"));
                    history.setVideoId(rs.getLong("video_id"));
                    history.setWatchProgress(rs.getInt("watch_progress"));
                    history.setWatchDuration(rs.getInt("watch_duration"));
                    history.setWatchTime(rs.getTimestamp("watch_time").toLocalDateTime());

                    LogUtil.logBusiness(logger, "SELECT_WATCH_HISTORY", "Found record");
                    return history;
                }
            }

            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_WATCH_HISTORY failed", e);
            throw new RuntimeException("查询观看记录失败", e);
        }
    }

    @Override
    public int deleteById(Connection conn, Long historyId) {
        String sql = "DELETE FROM user_watch_history WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{historyId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, historyId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_WATCH_HISTORY", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_WATCH_HISTORY failed", e);
            throw new RuntimeException("删除观看记录失败", e);
        }
    }

    @Override
    public int deleteByUserId(Connection conn, Long userId) {
        String sql = "DELETE FROM user_watch_history WHERE user_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{userId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_WATCH_HISTORY_BY_USER", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_WATCH_HISTORY_BY_USER failed", e);
            throw new RuntimeException("清空观看历史失败", e);
        }
    }
}