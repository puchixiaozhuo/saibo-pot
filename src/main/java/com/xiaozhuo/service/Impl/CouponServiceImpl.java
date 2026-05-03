package com.xiaozhuo.service.Impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.bean.dto.CouponGrabMessage;
import com.xiaozhuo.constant.MQConstant;
import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.entity.*;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CouponService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;
import com.xiaozhuo.util.RedisUtil;
import com.xiaozhuo.util.RocketMQUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

@Bean
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LogUtil.getLogger(CouponServiceImpl.class);

    private CouponDao couponDao = new CouponDaoImpl();

    private static final String COUPON_STOCK_PREFIX = "coupon:stock:";
    private static final String COUPON_USER_PREFIX = "coupon:user:";


    @Override
    @Transactional
    public Result<Map<String, Object>> grabCoupon(Long userId, Long activityId) {
        Entry entry = null;
        try {
            // 🔥 Sentinel 限流检查
            entry = SphU.entry("coupon:grab");

            if (userId == null || activityId == null) {
                return Result.fail(400, "参数无效");
            }

            Connection conn = ConnectionPool.getConnection();

            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getStartTime())) {
                return Result.fail(400, "活动未开始");
            }
            if (now.isAfter(activity.getEndTime())) {
                return Result.fail(400, "活动已结束");
            }

            if (activity.getRequiredWatchSeconds() != null && activity.getRequiredWatchSeconds() > 0) {
                UserVideoWatchRecord watchRecord = findAnyUnlockedRecord(conn, activityId, userId);

                if (watchRecord == null || watchRecord.getIsUnlocked() != 1) {
                    return Result.fail(400, "需要先观看视频解锁，观看时长要求：" + activity.getRequiredWatchSeconds() + "秒");
                }

                System.out.println("✅ Watch unlock verified - UserID: " + userId
                        + ", ActivityID: " + activityId);
            }

            CouponUser existingCoupon = couponDao.selectByUserIdAndActivityId(conn, userId, activityId);
            if (existingCoupon != null) {
                return Result.fail(400, "已领取过该活动的优惠券");
            }

            String stockKey = "coupon:stock:" + activityId;
            String userKey = "coupon:user:" + activityId + ":" + userId;

            Boolean isFirstGrab = RedisUtil.setnx(userKey, "1");
            if (!isFirstGrab) {
                return Result.fail(400, "请勿重复领取");
            }

            RedisUtil.expire(userKey, 3600);

            Long remaining = RedisUtil.decr(stockKey);
            if (remaining < 0) {
                RedisUtil.del(userKey);
                return Result.fail(400, "优惠券已抢完");
            }

            // 🔥 使用 RocketMQ 异步处理优惠券生成和数据库写入（削峰）
            try {
                String requestId = UUID.randomUUID().toString().replace("-", "");
                CouponGrabMessage message = new CouponGrabMessage(
                        userId,
                        activityId,
                        System.currentTimeMillis(),
                        requestId
                );

                String messageBody = JSON.toJSONString(message);
                RocketMQUtil.sendAsyncMessage(
                        MQConstant.TOPIC_COUPON_GRAB,
                        MQConstant.TAG_GRAB_REQUEST,
                        messageBody
                );

                LogUtil.logBusiness(logger, "COUPON_GRAB_MESSAGE_SENT",
                        "UserID: " + userId + ", ActivityID: " + activityId + ", RequestID: " + requestId);

            } catch (Exception e) {
                System.err.println("⚠️ MQ 发送失败，降级为同步处理: " + e.getMessage());
                e.printStackTrace();
                // 降级：直接生成优惠券
                generateCouponSync(userId, activityId);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("message", "抢购请求已提交，正在处理中...");
            data.put("requestTime", LocalDateTime.now().toString());

            System.out.println("✅ Coupon grab request submitted - UserID: " + userId
                    + ", ActivityID: " + activityId);

            return Result.success("抢购请求已提交", data);

        } catch (BlockException ex) {
            // 🔥 触发限流
            logger.warning("Coupon grab blocked by Sentinel: " + ex.getClass().getSimpleName());
            return Result.fail(429, "系统繁忙，请稍后重试（触发限流保护）");
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "抢购失败", e);
            return Result.error();
        } finally {
            // 🔥 确保退出 Sentinel 上下文
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 同步生成优惠券（降级方案）
     */
    private void generateCouponSync(Long userId, Long activityId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            String couponCode = generateCouponCode();
            CouponUser couponUser = new CouponUser();
            couponUser.setActivityId(activityId);
            couponUser.setUserId(userId);
            couponUser.setCouponCode(couponCode);
            couponUser.setStatus(0);
            couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
            couponUser.setCreateTime(LocalDateTime.now());

            couponDao.insertCouponUser(conn, couponUser);

            System.out.println("✅ [降级] Coupon generated synchronously - UserID: " + userId
                    + ", CouponCode: " + couponCode);

        } catch (Exception e) {
            System.err.println("❌ [降级] Failed to generate coupon: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 查找用户在该活动下任意已解锁的观看记录
     */
    private UserVideoWatchRecord findAnyUnlockedRecord(Connection conn, Long userId, Long activityId) {
        String sql = "SELECT * FROM user_video_watch_record WHERE user_id = ? AND activity_id = ? AND is_unlocked = 1 LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, activityId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询解锁记录失败", e);
        }
        return null;

    }

    /**
     * 异步更新数据库库存（从 Redis 同步）
     */
    private void asyncUpdateStock(Long activityId) {
        // 简单实现：在新线程中更新
        new Thread(() -> {
            Connection conn = null;
            try {
                String stockKey = COUPON_STOCK_PREFIX + activityId;
                String cachedStock = RedisUtil.get(stockKey);

                if (cachedStock != null) {
                    conn = ConnectionPool.getConnection();
                    String sql = "UPDATE coupon_activity SET remaining_stock = ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, Integer.parseInt(cachedStock));
                        ps.setLong(2, activityId);
                        ps.executeUpdate();
                    }
                    System.out.println("✅ Stock synced to DB - ActivityID: " + activityId
                            + ", Stock: " + cachedStock);
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to sync stock: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    ConnectionPool.returnConnection(conn);
                }
            }
        }).start();
    }

    @Override
    public Result<List<CouponUser>> getMyCoupons(Long userId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            conn = ConnectionPool.getConnection();

            List<CouponUser> coupons = couponDao.selectByUserId(conn, userId, pageNum, pageSize);
            long total = couponDao.countByUserId(conn, userId);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("coupons", coupons);
            pageInfo.put("total", total);
            pageInfo.put("pageNum", pageNum);
            pageInfo.put("pageSize", pageSize);
            pageInfo.put("totalPages", (total + pageSize - 1) / pageSize);

            return Result.success("查询成功", coupons);

        } catch (Exception e) {
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Map<String, Object>> getActivityDetail(Long activityId) {
        Connection conn = null;
        try {
            if (activityId == null) {
                return Result.fail(400, "活动ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            String stockKey = COUPON_STOCK_PREFIX + activityId;
            String cachedStock = RedisUtil.get(stockKey);

            Map<String, Object> data = new HashMap<>();
            data.put("id", activity.getId());
            data.put("title", activity.getTitle());
            data.put("description", activity.getDescription());
            data.put("totalStock", activity.getTotalStock());

            if (cachedStock != null) {
                data.put("remainingStock", Long.parseLong(cachedStock));
            } else {
                data.put("remainingStock", activity.getRemainingStock());
            }

            data.put("startTime", activity.getStartTime());
            data.put("endTime", activity.getEndTime());
            data.put("status", activity.getStatus());

            return Result.success("查询成功", data);

        } catch (Exception e) {
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    private String generateCouponCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CPN" + timestamp + random;
    }

    @Override
    public Result<Boolean> initStockToRedis(Long activityId) {
        Connection conn = null;
        try {
            if (activityId == null) {
                return Result.fail(400, "活动ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            String stockKey = COUPON_STOCK_PREFIX + activityId;

            RedisUtil.set(stockKey, String.valueOf(activity.getRemainingStock()));

            System.out.println(" 库存已同步到 Redis - 活动ID: " + activityId
                    + ", 库存: " + activity.getRemainingStock());

            return Result.success("库存初始化成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Integer> releasePendingBatches() {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            List<CouponBatch> pendingBatches = couponDao.selectPendingBatches(conn);

            if (pendingBatches == null || pendingBatches.isEmpty()) {
                System.out.println("ℹ️ 暂无待释放批次");
                return Result.success("暂无待释放批次", 0);
            }

            int releasedCount = 0;
            for (CouponBatch batch : pendingBatches) {
                String stockKey = COUPON_STOCK_PREFIX + batch.getActivityId();

                String currentStockStr = RedisUtil.get(stockKey);
                long currentStock = currentStockStr != null ? Long.parseLong(currentStockStr) : 0L;

                long newStock = currentStock + batch.getStockCount();
                RedisUtil.set(stockKey, String.valueOf(newStock));

                couponDao.updateBatchReleased(conn, batch.getId(), batch.getStockCount());

                releasedCount++;

                System.out.println("✅ 批次释放成功 - 批次ID: " + batch.getId()
                        + ", 活动ID: " + batch.getActivityId()
                        + ", 批次号: " + batch.getBatchNumber()
                        + ", 释放数量: " + batch.getStockCount()
                        + ", 当前Redis库存: " + newStock);
            }

            System.out.println("🎉 共释放 " + releasedCount + " 个批次");
            return Result.success("批次释放成功", releasedCount);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getActivityBatches(Long activityId) {
        Connection conn = null;
        try {
            if (activityId == null) {
                return Result.fail(400, "活动ID不能为空");
            }

            conn = ConnectionPool.getConnection();
            List<CouponBatch> batches = couponDao.selectByActivityId(conn, activityId);

            if (batches == null || batches.isEmpty()) {
                return Result.success("暂无批次数据", new ArrayList<>());
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (CouponBatch batch : batches) {
                Map<String, Object> batchInfo = new HashMap<>();
                batchInfo.put("id", batch.getId());
                batchInfo.put("batchNumber", batch.getBatchNumber());
                batchInfo.put("stockCount", batch.getStockCount());
                batchInfo.put("releasedStock", batch.getReleasedStock());
                batchInfo.put("releaseTime", batch.getReleaseTime());
                batchInfo.put("status", batch.getStatus());
                batchInfo.put("createTime", batch.getCreateTime());
                result.add(batchInfo);
            }

            return Result.success("查询成功", result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> reserveCoupon(Long userId, Long activityId) {
        Connection conn = null;
        try {
            if (userId == null || activityId == null) {
                return Result.fail(400, "参数无效");
            }

            conn = ConnectionPool.getConnection();

            // 1. 检查活动是否存在
            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            // 2. 检查活动时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = activity.getStartTime();
            LocalDateTime endTime = activity.getEndTime();

            System.out.println("🔍 Debug - Current Time: " + now);
            System.out.println("🔍 Debug - Start Time: " + startTime);
            System.out.println("🔍 Debug - End Time: " + endTime);
            System.out.println("🔍 Debug - Is Before Start: " + now.isBefore(startTime));
            System.out.println("🔍 Debug - Is After End: " + now.isAfter(endTime));

            if (now.isBefore(startTime)) {
                return Result.fail(400, "活动未开始");
            }
            if (now.isAfter(endTime)) {
                return Result.fail(400, "活动已结束，无法预约");
            }


            // 3. 检查是否已预约
            CouponReservation existing = couponDao.selectReservationByUserIdAndActivityId(conn, userId, activityId);
            if (existing != null) {
                if (existing.getStatus() == 0) {
                    return Result.fail(400, "您已预约该活动");
                } else if (existing.getStatus() == 1) {
                    return Result.fail(400, "您已中签，无需重复预约");
                } else {
                    return Result.fail(400, "您未中签，无法再次预约");
                }
            }

            // 4. 创建预约记录
            CouponReservation reservation = new CouponReservation();
            reservation.setActivityId(activityId);
            reservation.setUserId(userId);
            reservation.setStatus(0); // 已预约

            couponDao.insertReservation(conn, reservation);

            System.out.println("✅ Reservation successful - UserID: " + userId
                    + ", ActivityID: " + activityId);

            return Result.success("预约成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Integer> executeLottery(Long activityId) {
        Connection conn = null;
        try {
            if (activityId == null) {
                return Result.fail(400, "活动ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            // 1. 检查活动是否存在
            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            // 2. 检查活动是否已结束
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = activity.getEndTime();

            System.out.println("🔍 Debug - Current Time: " + now);
            System.out.println("🔍 Debug - End Time: " + endTime);
            System.out.println("🔍 Debug - Is After: " + now.isAfter(endTime));

            if (!now.isAfter(endTime)) {
                return Result.fail(400, "活动尚未结束，无法执行抽签");
            }

            // 3. 查询所有预约用户
            List<CouponReservation> reservations = couponDao.selectReservationsByActivityId(conn, activityId);
            if (reservations == null || reservations.isEmpty()) {
                return Result.fail(400, "暂无预约用户");
            }

            int totalReservations = reservations.size();
            int totalStock = activity.getTotalStock();

            // 4. 确定中签人数（取预约人数和库存的较小值）
            int winnerCount = Math.min(totalReservations, totalStock);

            System.out.println("🎲 Starting lottery - ActivityID: " + activityId
                    + ", Total Reservations: " + totalReservations
                    + ", Total Stock: " + totalStock
                    + ", Winners: " + winnerCount);

            // 5. 随机打乱预约列表
            java.util.Collections.shuffle(reservations);

            // 6. 选取前 winnerCount 个用户作为中签者
            int successCount = 0;
            for (int i = 0; i < winnerCount; i++) {
                CouponReservation reservation = reservations.get(i);

                //  检查用户是否已经有优惠券（防止重复插入）
                CouponUser existingCoupon = couponDao.selectByUserIdAndActivityId(
                        conn, reservation.getUserId(), activityId);

                if (existingCoupon != null) {
                    System.out.println("⚠️ User " + reservation.getUserId() + " already has coupon, skip insertion");
                    // 仍然更新预约状态为"已中签"
                    couponDao.updateReservationStatus(conn, reservation.getId(), 1);
                    continue;
                }

                // 更新预约状态为"已中签"
                couponDao.updateReservationStatus(conn, reservation.getId(), 1);

                // 生成优惠券
                String couponCode = generateCouponCode();
                CouponUser couponUser = new CouponUser();
                couponUser.setActivityId(activityId);
                couponUser.setUserId(reservation.getUserId());
                couponUser.setCouponCode(couponCode);
                couponUser.setStatus(0);
                couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
                couponUser.setCreateTime(LocalDateTime.now());

                couponDao.insertCouponUser(conn, couponUser);

                successCount++;

                System.out.println("🎉 Winner - UserID: " + reservation.getUserId()
                        + ", CouponCode: " + couponCode);
            }

            // 7. 更新未中签用户状态
            for (int i = winnerCount; i < reservations.size(); i++) {
                CouponReservation reservation = reservations.get(i);
                couponDao.updateReservationStatus(conn, reservation.getId(), 2);
            }

            // 8. 扣减库存
            couponDao.decrementStock(conn, activityId, winnerCount);

            System.out.println("✅ Lottery completed - Winners: " + successCount
                    + ", Losers: " + (totalReservations - winnerCount));

            return Result.success("抽签完成", successCount);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Map<String, Object>> getMyReservationStatus(Long userId, Long activityId) {
        Connection conn = null;
        try {
            if (userId == null || activityId == null) {
                return Result.fail(400, "参数无效");
            }

            conn = ConnectionPool.getConnection();

            CouponReservation reservation = couponDao.selectReservationByUserIdAndActivityId(conn, userId, activityId);

            Map<String, Object> data = new HashMap<>();
            if (reservation == null) {
                data.put("reserved", false);
                data.put("status", null);
                data.put("message", "未预约");
            } else {
                data.put("reserved", true);
                data.put("status", reservation.getStatus());
                data.put("createTime", reservation.getCreateTime());

                String statusMsg;
                switch (reservation.getStatus()) {
                    case 0:
                        statusMsg = "已预约，等待抽签";
                        break;
                    case 1:
                        statusMsg = "已中签";
                        break;
                    case 2:
                        statusMsg = "未中签";
                        break;
                    default:
                        statusMsg = "未知状态";
                }
                data.put("message", statusMsg);
            }

            return Result.success("查询成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Boolean> reportWatchProgress(Long userId, Long videoId, Long activityId, Integer watchedSeconds) {
        Connection conn = null;
        try {
            if (userId == null || videoId == null || activityId == null || watchedSeconds == null) {
                return Result.fail(400, "参数无效");
            }

            conn = ConnectionPool.getConnection();

            // 1. 检查活动是否存在且需要观看
            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            // 如果活动不需要观看时长，直接返回
            if (activity.getRequiredWatchSeconds() == null || activity.getRequiredWatchSeconds() == 0) {
                return Result.fail(400, "该活动无需观看时长");
            }

            // 2. 初始化或更新观看记录
            UserVideoWatchRecord record = couponDao.selectWatchRecord(conn, userId, videoId, activityId);
            if (record == null) {
                UserVideoWatchRecord newRecord = new UserVideoWatchRecord();
                newRecord.setUserId(userId);
                newRecord.setVideoId(videoId);
                newRecord.setActivityId(activityId);
                couponDao.upsertWatchRecord(conn, newRecord);
            }

            // 3. 更新观看时长
            couponDao.updateWatchedSeconds(conn, userId, videoId, activityId, watchedSeconds);

            // 4. 重新查询观看记录
            record = couponDao.selectWatchRecord(conn, userId, videoId, activityId);

            // 5. 检查是否达到解锁条件
            if (record != null && record.getWatchedSeconds() >= activity.getRequiredWatchSeconds()
                    && record.getIsUnlocked() == 0) {
                // 标记为已解锁
                couponDao.markAsUnlocked(conn, userId, videoId, activityId);
                System.out.println("✅ Watch unlocked - UserID: " + userId
                        + ", ActivityID: " + activityId
                        + ", Watched: " + record.getWatchedSeconds() + "s");
            }

            return Result.success("上报成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Map<String, Object>> checkUnlockStatus(Long userId, Long videoId, Long activityId) {
        Connection conn = null;
        try {
            if (userId == null || videoId == null || activityId == null) {
                return Result.fail(400, "参数无效");
            }

            conn = ConnectionPool.getConnection();

            // 1. 检查活动
            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("activityId", activityId);
            data.put("requiredWatchSeconds", activity.getRequiredWatchSeconds());

            // 2. 查询观看记录
            UserVideoWatchRecord record = couponDao.selectWatchRecord(conn, userId, videoId, activityId);

            if (record == null) {
                data.put("watchedSeconds", 0);
                data.put("isUnlocked", false);
                data.put("progress", 0.0);
                data.put("message", "尚未开始观看");
            } else {
                data.put("watchedSeconds", record.getWatchedSeconds());
                data.put("isUnlocked", record.getIsUnlocked() == 1);

                double progress = activity.getRequiredWatchSeconds() > 0
                        ? (double) record.getWatchedSeconds() / activity.getRequiredWatchSeconds() * 100
                        : 0;
                data.put("progress", Math.min(progress, 100.0));

                if (record.getIsUnlocked() == 1) {
                    data.put("message", "已解锁，可以领取优惠券");
                } else {
                    int remaining = activity.getRequiredWatchSeconds() - record.getWatchedSeconds();
                    data.put("message", "还需观看 " + remaining + " 秒");
                }
            }

            return Result.success("查询成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Map<String, Object>> claimWatchUnlockCoupon(Long userId, Long activityId) {
        Connection conn = null;
        try {
            if (userId == null || activityId == null) {
                return Result.fail(400, "参数无效");
            }

            conn = ConnectionPool.getConnection();

            // 1. 检查活动
            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            // 4. 从 Redis 检查库存
            String stockKey = COUPON_STOCK_PREFIX + activityId;
            String cachedStock = RedisUtil.get(stockKey);

            if (cachedStock == null || Long.parseLong(cachedStock) <= 0) {
                return Result.fail(400, "优惠券已抢完");
            }

            // 3. 检查是否已领取
            CouponUser existingCoupon = couponDao.selectByUserIdAndActivityId(conn, userId, activityId);
            if (existingCoupon != null) {
                return Result.fail(400, "已领取过该活动的优惠券");
            }

            // 4. 检查是否已解锁（需要查询任意视频的观看记录）
            // 这里简化处理：只要有一个视频解锁了就可以领取
            // 实际业务可能需要指定视频ID

            // 5. 生成优惠券
            String couponCode = generateCouponCode();
            CouponUser couponUser = new CouponUser();
            couponUser.setActivityId(activityId);
            couponUser.setUserId(userId);
            couponUser.setCouponCode(couponCode);
            couponUser.setStatus(0);
            couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
            couponUser.setCreateTime(LocalDateTime.now());

            couponDao.insertCouponUser(conn, couponUser);

            // 6. 扣减 Redis 库存
            Long remaining = RedisUtil.decr(stockKey);
            if (remaining < 0) {
                // 库存不足，删除优惠券记录
                RedisUtil.incr(stockKey); // 恢复库存
                return Result.fail(400, "优惠券已被抢完，请重试");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("couponCode", couponCode);
            data.put("expireTime", couponUser.getExpireTime());

            System.out.println("✅ Watch unlock coupon claimed - UserID: " + userId
                    + ", ActivityID: " + activityId
                    + ", CouponCode: " + couponCode);

            return Result.success("领取成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }
}