package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.entity.CouponActivity;
import com.xiaozhuo.entity.CouponUser;
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

    private CouponActivity mapToActivity(ResultSet rs) throws SQLException {
        CouponActivity activity = new CouponActivity();
        activity.setId(rs.getLong("id"));
        activity.setVideoId(rs.getLong("video_id"));
        activity.setTitle(rs.getString("title"));
        activity.setDescription(rs.getString("description"));
        activity.setTotalStock(rs.getInt("total_stock"));
        activity.setRemainingStock(rs.getInt("remaining_stock"));
        activity.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        activity.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        activity.setStatus(rs.getInt("status"));
        activity.setVersion(rs.getInt("version"));
        activity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        activity.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
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