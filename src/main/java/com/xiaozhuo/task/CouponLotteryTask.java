package com.xiaozhuo.task;

import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.entity.CouponActivity;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 优惠券抽签定时任务
 * 定期检查已结束但未抽签的活动，自动执行抽签
 */
public class CouponLotteryTask {

    private static final Logger logger = LogUtil.getLogger(CouponLotteryTask.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final CouponDao couponDao = new CouponDaoImpl();

    /**
     * 启动定时任务
     */
    public static void start() {
        logger.info("🚀 Coupon lottery task starting...");

        // 每5分钟检查一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndExecuteLottery();
            } catch (Exception e) {
                logger.severe("❌ Lottery task execution failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);

        logger.info("✅ Coupon lottery task started (check interval: 5 minutes)");
    }

    /**
     * 检查并执行抽签
     */
    private static void checkAndExecuteLottery() {
        logger.info("🔍 Checking for pending lotteries...");

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            // 查询所有状态为进行中且已结束的活动
            List<CouponActivity> activities = couponDao.selectEndedActivities(conn);

            if (activities == null || activities.isEmpty()) {
                logger.info("ℹ️ No activities need lottery");
                return;
            }

            logger.info("📊 Found " + activities.size() + " activities need lottery");

            // 对每个活动执行抽签
            for (CouponActivity activity : activities) {
                try {
                    executeLotteryForActivity(activity);
                } catch (Exception e) {
                    logger.severe("❌ Failed to execute lottery for activity " + activity.getId()
                        + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            logger.severe("❌ Check lottery task failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 为单个活动执行抽签
     */
    private static void executeLotteryForActivity(CouponActivity activity) {
        Long activityId = activity.getId();
        logger.info("🎲 Starting lottery for activity " + activityId + ": " + activity.getTitle());

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            // 查询所有预约用户
            List<com.xiaozhuo.entity.CouponReservation> reservations =
                couponDao.selectReservationsByActivityId(conn, activityId);

            if (reservations == null || reservations.isEmpty()) {
                logger.info("ℹ️ No reservations for activity " + activityId);
                return;
            }

            int totalReservations = reservations.size();
            int totalStock = activity.getTotalStock();
            int winnerCount = Math.min(totalReservations, totalStock);

            logger.info("📊 Activity " + activityId + " - Reservations: " + totalReservations
                + ", Stock: " + totalStock + ", Winners: " + winnerCount);

            // 随机打乱
            java.util.Collections.shuffle(reservations);

            // 处理中签用户
            int successCount = 0;
            for (int i = 0; i < winnerCount; i++) {
                com.xiaozhuo.entity.CouponReservation reservation = reservations.get(i);

                // 检查是否已有优惠券
                com.xiaozhuo.entity.CouponUser existingCoupon =
                    couponDao.selectByUserIdAndActivityId(conn, reservation.getUserId(), activityId);

                if (existingCoupon != null) {
                    logger.info("⚠️ User " + reservation.getUserId() + " already has coupon");
                    couponDao.updateReservationStatus(conn, reservation.getId(), 1);
                    continue;
                }

                // 更新预约状态
                couponDao.updateReservationStatus(conn, reservation.getId(), 1);

                // 生成优惠券
                String couponCode = generateCouponCode();
                com.xiaozhuo.entity.CouponUser couponUser = new com.xiaozhuo.entity.CouponUser();
                couponUser.setActivityId(activityId);
                couponUser.setUserId(reservation.getUserId());
                couponUser.setCouponCode(couponCode);
                couponUser.setStatus(0);
                couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
                couponUser.setCreateTime(LocalDateTime.now());

                couponDao.insertCouponUser(conn, couponUser);
                successCount++;

                logger.info("🎉 Winner - UserID: " + reservation.getUserId()
                    + ", CouponCode: " + couponCode);
            }

            // 处理未中签用户
            for (int i = winnerCount; i < reservations.size(); i++) {
                com.xiaozhuo.entity.CouponReservation reservation = reservations.get(i);
                couponDao.updateReservationStatus(conn, reservation.getId(), 2);
            }

            // 扣减库存
            couponDao.decrementStock(conn, activityId, winnerCount);

            logger.info("✅ Lottery completed for activity " + activityId
                + " - Winners: " + successCount
                + ", Losers: " + (totalReservations - winnerCount));

        } catch (Exception e) {
            logger.severe("❌ Execute lottery failed for activity " + activityId);
            e.printStackTrace();
            throw new RuntimeException("Execute lottery failed", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 生成优惠券码
     */
    private static String generateCouponCode() {
        String timestamp = LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = java.util.UUID.randomUUID().toString()
            .replace("-", "").substring(0, 8).toUpperCase();
        return "CPN" + timestamp + random;
    }

    /**
     * 停止定时任务
     */
    public static void stop() {
        scheduler.shutdown();
        logger.info("🛑 Coupon lottery task stopped");
    }
}
