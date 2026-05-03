package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.entity.*;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CouponDaoImpl implements CouponDao {

    private static final Logger logger = LogUtil.getLogger(CouponDaoImpl.class);

    @Override
    public CouponActivity selectById(Connection conn, Long id) {
        String sql = "SELECT * FROM coupon_activity WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapToActivity(rs);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询活动失败: id=" + id, e);
            throw new RuntimeException("查询活动失败", e);
        }

        return null;
    }

    @Override
    public int decrementStockWithOptimisticLock(Connection conn, Long activityId, Integer version) {
        String sql = "UPDATE coupon_activity SET remaining_stock = remaining_stock - 1, version = version + 1 WHERE id = ? AND remaining_stock > 0 AND version = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, activityId);
            pstmt.setInt(2, version);

            int rows = pstmt.executeUpdate();

            if (rows == 0) {
                LogUtil.logBusiness(logger, "OPTIMISTIC_LOCK_FAIL",
                    "Activity " + activityId + " stock decrement failed (stock=0 or version conflict)");
            } else {
                LogUtil.logBusiness(logger, "STOCK_DECREMENT",
                    "Activity " + activityId + " stock decremented successfully");
            }

            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "扣减库存失败: activityId=" + activityId, e);
            throw new RuntimeException("扣减库存失败", e);
        }
    }

    @Override
    public int insertCouponUser(Connection conn, CouponUser couponUser) {
        String sql = "INSERT INTO coupon_user (activity_id, user_id, coupon_code, status, expire_time, create_time) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, couponUser.getActivityId());
            pstmt.setLong(2, couponUser.getUserId());
            pstmt.setString(3, couponUser.getCouponCode());
            pstmt.setInt(4, couponUser.getStatus());
            pstmt.setTimestamp(5, Timestamp.valueOf(couponUser.getExpireTime()));
            pstmt.setTimestamp(6, Timestamp.valueOf(couponUser.getCreateTime()));

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "INSERT_COUPON",
                "User " + couponUser.getUserId() + " received coupon: " + couponUser.getCouponCode());

            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入优惠券记录失败", e);
            throw new RuntimeException("插入优惠券记录失败", e);
        }
    }

    @Override
    public CouponUser selectByUserIdAndActivityId(Connection conn, Long userId, Long activityId) {
        String sql = "SELECT * FROM coupon_user WHERE user_id = ? AND activity_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, activityId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapToCouponUser(rs);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询用户优惠券失败", e);
            throw new RuntimeException("查询用户优惠券失败", e);
        }

        return null;
    }

    @Override
    public List<CouponUser> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM coupon_user WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setInt(2, pageSize);
            pstmt.setInt(3, offset);

            List<CouponUser> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToCouponUser(rs));
                }
            }

            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询用户优惠券列表失败", e);
            throw new RuntimeException("查询用户优惠券列表失败", e);
        }
    }

    @Override
    public long countByUserId(Connection conn, Long userId) {
        String sql = "SELECT COUNT(*) FROM coupon_user WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计用户优惠券数量失败", e);
            throw new RuntimeException("统计用户优惠券数量失败", e);
        }

        return 0;
    }

    @Override
    public List<CouponBatch> selectPendingBatches(Connection conn) {
        String sql = "SELECT * FROM coupon_batch WHERE status = 0 AND release_time <= NOW() ORDER BY release_time ASC";
        List<CouponBatch> batches = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                batches.add(mapBatchRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询待释放批次失败", e);
            throw new DatabaseException("查询待释放批次失败", e);
        }
        return batches;
    }

    @Override
    public int updateBatchReleased(Connection conn, Long batchId, Integer releasedStock) {
        String sql = "UPDATE coupon_batch SET released_stock = ?, status = 1, update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, releasedStock);
            ps.setLong(2, batchId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "更新批次状态失败: batchId=" + batchId, e);
            throw new DatabaseException("更新批次状态失败", e);
        }
    }

    @Override
    public List<CouponBatch> selectByActivityId(Connection conn, Long activityId) {
        String sql = "SELECT * FROM coupon_batch WHERE activity_id = ? ORDER BY batch_number ASC";
        List<CouponBatch> batches = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, activityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                batches.add(mapBatchRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询活动批次失败: activityId=" + activityId, e);
            throw new DatabaseException("查询活动批次失败", e);
        }
        return batches;
    }

    @Override
    public CouponReservation selectReservationByUserIdAndActivityId(Connection conn, Long userId, Long activityId) {
        String sql = "SELECT * FROM coupon_reservation WHERE user_id = ? AND activity_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapReservationRow(rs);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询预约记录失败: userId=" + userId + ", activityId=" + activityId, e);
            throw new DatabaseException("查询预约记录失败", e);
        }
        return null;
    }

    @Override
    public List<CouponReservation> selectReservationsByActivityId(Connection conn, Long activityId) {
        String sql = "SELECT * FROM coupon_reservation WHERE activity_id = ? ORDER BY create_time ASC";
        List<CouponReservation> reservations = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, activityId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservationRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询活动预约列表失败: activityId=" + activityId, e);
            throw new DatabaseException("查询活动预约列表失败", e);
        }
        return reservations;
    }

    @Override
    public int insertReservation(Connection conn, CouponReservation reservation) {
        String sql = "INSERT INTO coupon_reservation (activity_id, user_id, status, create_time) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservation.getActivityId());
            ps.setLong(2, reservation.getUserId());
            ps.setInt(3, reservation.getStatus() != null ? reservation.getStatus() : 0);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入预约记录失败", e);
            throw new DatabaseException("插入预约记录失败", e);
        }
    }

    @Override
    public int updateReservationStatus(Connection conn, Long reservationId, Integer status) {
        String sql = "UPDATE coupon_reservation SET status = ?, update_time = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, status);
            ps.setLong(2, reservationId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "更新预约状态失败: reservationId=" + reservationId, e);
            throw new DatabaseException("更新预约状态失败", e);
        }
    }

    @Override
    public int countReservationsByActivityId(Connection conn, Long activityId) {
        String sql = "SELECT COUNT(*) FROM coupon_reservation WHERE activity_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计预约人数失败: activityId=" + activityId, e);
            throw new DatabaseException("统计预约人数失败", e);
        }
        return 0;
    }

    @Override
    public int decrementStock(Connection conn, Long activityId, Integer amount) {
        String sql = "UPDATE coupon_activity SET remaining_stock = remaining_stock - ?, version = version + 1 WHERE id = ? AND remaining_stock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setLong(2, activityId);
            ps.setInt(3, amount);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "扣减库存失败: activityId=" + activityId, e);
            throw new DatabaseException("扣减库存失败", e);
        }
    }
    @Override
    public List<CouponActivity> selectEndedActivities(Connection conn) {
        String sql = "SELECT * FROM coupon_activity WHERE status = 1 AND end_time <= NOW() ORDER BY end_time ASC";
        List<CouponActivity> activities = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                activities.add(mapToActivity(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询已结束活动失败", e);
            throw new DatabaseException("查询已结束活动失败", e);
        }
        return activities;
    }

    @Override
    public int upsertWatchRecord(Connection conn, UserVideoWatchRecord record) {
        String sql = "INSERT INTO user_video_watch_record (user_id, video_id, activity_id, watched_seconds, is_unlocked, create_time) " +
                "VALUES (?, ?, ?, 0, 0, NOW()) " +
                "ON DUPLICATE KEY UPDATE update_time = NOW()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, record.getUserId());
            ps.setLong(2, record.getVideoId());
            ps.setLong(3, record.getActivityId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入观看记录失败", e);
            throw new DatabaseException("插入观看记录失败", e);
        }
    }

    @Override
    public UserVideoWatchRecord selectWatchRecord(Connection conn, Long userId, Long videoId, Long activityId) {
        String sql = "SELECT * FROM user_video_watch_record WHERE user_id = ? AND video_id = ? AND activity_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, videoId);
            ps.setLong(3, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapWatchRecordRow(rs);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询观看记录失败", e);
            throw new DatabaseException("查询观看记录失败", e);
        }
        return null;
    }

    @Override
    public int updateWatchedSeconds(Connection conn, Long userId, Long videoId, Long activityId, Integer additionalSeconds) {
        String sql = "UPDATE user_video_watch_record SET watched_seconds = watched_seconds + ?, update_time = NOW() " +
                "WHERE user_id = ? AND video_id = ? AND activity_id = ? AND is_unlocked = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, additionalSeconds);
            ps.setLong(2, userId);
            ps.setLong(3, videoId);
            ps.setLong(4, activityId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "更新观看时长失败", e);
            throw new DatabaseException("更新观看时长失败", e);
        }
    }

    @Override
    public int markAsUnlocked(Connection conn, Long userId, Long videoId, Long activityId) {
        String sql = "UPDATE user_video_watch_record SET is_unlocked = 1, unlock_time = NOW(), update_time = NOW() " +
                "WHERE user_id = ? AND video_id = ? AND activity_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, videoId);
            ps.setLong(3, activityId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "标记解锁失败", e);
            throw new DatabaseException("标记解锁失败", e);
        }
    }

    @Override
    public int insertActivity(Connection conn, CouponActivity activity) {
        String sql = "INSERT INTO coupon_activity (video_id, activity_type, title, description, discount_content, total_stock, remaining_stock, start_time, end_time, status, version, required_watch_seconds, batch_config, lottery_config, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, activity.getVideoId());
            pstmt.setInt(2, activity.getActivityType());
            pstmt.setString(3, activity.getTitle());
            pstmt.setString(4, activity.getDescription());
            pstmt.setString(5, activity.getDiscountContent());
            pstmt.setInt(6, activity.getTotalStock());
            pstmt.setInt(7, activity.getRemainingStock());
            pstmt.setTimestamp(8, Timestamp.valueOf(activity.getStartTime()));
            pstmt.setTimestamp(9, Timestamp.valueOf(activity.getEndTime()));
            pstmt.setInt(10, activity.getStatus());
            pstmt.setInt(11, activity.getVersion());
            pstmt.setInt(12, activity.getRequiredWatchSeconds());
            pstmt.setString(13, activity.getBatchConfig());
            pstmt.setString(14, activity.getLotteryConfig());

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        activity.setId(generatedKeys.getLong(1));
                    }
                }
            }

            LogUtil.logBusiness(logger, "INSERT_ACTIVITY",
                    "Created coupon activity: " + activity.getTitle() + " for video " + activity.getVideoId());

            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入优惠券活动失败", e);
            throw new DatabaseException("插入优惠券活动失败", e);
        }
    }

    @Override
    public int insertBatch(Connection conn, CouponBatch batch) {
        String sql = "INSERT INTO coupon_batch (activity_id, batch_number, stock_count, released_stock, release_time, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, batch.getActivityId());
            pstmt.setInt(2, batch.getBatchNumber());
            pstmt.setInt(3, batch.getStockCount());
            pstmt.setInt(4, batch.getReleasedStock());
            pstmt.setTimestamp(5, Timestamp.valueOf(batch.getReleaseTime()));
            pstmt.setInt(6, batch.getStatus());

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        batch.setId(generatedKeys.getLong(1));
                    }
                }
            }

            LogUtil.logBusiness(logger, "INSERT_BATCH",
                    "Created batch " + batch.getBatchNumber() + " for activity " + batch.getActivityId());

            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入批次记录失败", e);
            throw new DatabaseException("插入批次记录失败", e);
        }
    }

    private UserVideoWatchRecord mapWatchRecordRow(ResultSet rs) throws SQLException {
        UserVideoWatchRecord record = new UserVideoWatchRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getLong("user_id"));
        record.setVideoId(rs.getLong("video_id"));
        record.setActivityId(rs.getLong("activity_id"));
        record.setWatchedSeconds(rs.getInt("watched_seconds"));
        record.setIsUnlocked(rs.getInt("is_unlocked"));

        Timestamp unlockTime = rs.getTimestamp("unlock_time");
        if (unlockTime != null) {
            record.setUnlockTime(unlockTime.toLocalDateTime());
        }

        record.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        record.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        return record;
    }

    private CouponReservation mapReservationRow(ResultSet rs) throws SQLException {
        CouponReservation reservation = new CouponReservation();
        reservation.setId(rs.getLong("id"));
        reservation.setActivityId(rs.getLong("activity_id"));
        reservation.setUserId(rs.getLong("user_id"));
        reservation.setStatus(rs.getInt("status"));
        reservation.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        reservation.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        return reservation;
    }


    private CouponBatch mapBatchRow(ResultSet rs) throws SQLException {
        CouponBatch batch = new CouponBatch();
        batch.setId(rs.getLong("id"));
        batch.setActivityId(rs.getLong("activity_id"));
        batch.setBatchNumber(rs.getInt("batch_number"));
        batch.setStockCount(rs.getInt("stock_count"));
        batch.setReleasedStock(rs.getInt("released_stock"));
        batch.setReleaseTime(rs.getTimestamp("release_time").toLocalDateTime());

        Timestamp actualReleaseTime = rs.getTimestamp("actual_release_time");
        if (actualReleaseTime != null) {
            batch.setActualReleaseTime(actualReleaseTime.toLocalDateTime());
        }

        batch.setStatus(rs.getInt("status"));
        batch.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        batch.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        return batch;
    }


    private CouponActivity mapToActivity(ResultSet rs) throws SQLException {
        CouponActivity activity = new CouponActivity();
        activity.setId(rs.getLong("id"));
        activity.setVideoId(rs.getLong("video_id"));
        activity.setActivityType(rs.getInt("activity_type"));
        activity.setTitle(rs.getString("title"));
        activity.setDescription(rs.getString("description"));
        activity.setDiscountContent(rs.getString("discount_content"));
        activity.setTotalStock(rs.getInt("total_stock"));
        activity.setRemainingStock(rs.getInt("remaining_stock"));
        activity.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        activity.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        activity.setStatus(rs.getInt("status"));
        activity.setVersion(rs.getInt("version"));
        activity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        activity.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        activity.setRequiredWatchSeconds(rs.getInt("required_watch_seconds"));
        activity.setBatchConfig(rs.getString("batch_config"));
        activity.setLotteryConfig(rs.getString("lottery_config"));

        return activity;
    }

    private CouponUser mapToCouponUser(ResultSet rs) throws SQLException {
        CouponUser coupon = new CouponUser();
        coupon.setId(rs.getLong("id"));
        coupon.setActivityId(rs.getLong("activity_id"));
        coupon.setUserId(rs.getLong("user_id"));
        coupon.setCouponCode(rs.getString("coupon_code"));
        coupon.setStatus(rs.getInt("status"));

        Timestamp useTime = rs.getTimestamp("use_time");
        if (useTime != null) {
            coupon.setUseTime(useTime.toLocalDateTime());
        }

        coupon.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
        coupon.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return coupon;
    }
}