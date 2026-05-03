package com.xiaozhuo.task;

import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.entity.CouponBatch;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;
import com.xiaozhuo.util.RedisUtil;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 优惠券批次自动释放定时任务
 * 定期检查到达释放时间的批次，自动将库存添加到 Redis
 */
public class CouponBatchReleaseTask {

    private static final Logger logger = LogUtil.getLogger(CouponBatchReleaseTask.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final CouponDao couponDao = new CouponDaoImpl();

    private static final String COUPON_STOCK_PREFIX = "coupon:stock:";

    /**
     * 启动定时任务
     */
    public static void start() {
        logger.info("🚀 Coupon batch release task starting...");

        // 每1分钟检查一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndReleaseBatches();
            } catch (Exception e) {
                logger.severe("❌ Batch release task execution failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);

        logger.info("✅ Coupon batch release task started (check interval: 1 minute)");
    }

    /**
     * 检查并释放批次
     */
    private static void checkAndReleaseBatches() {
        logger.info("🔍 Checking for pending batch releases...");

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            // 查询所有待释放且到达释放时间的批次
            List<CouponBatch> pendingBatches = couponDao.selectPendingBatches(conn);

            if (pendingBatches == null || pendingBatches.isEmpty()) {
                logger.info("ℹ️ No batches need to be released");
                return;
            }

            logger.info("📊 Found " + pendingBatches.size() + " batches to release");

            // 对每个批次执行释放
            for (CouponBatch batch : pendingBatches) {
                try {
                    releaseBatch(batch);
                } catch (Exception e) {
                    logger.severe("❌ Failed to release batch " + batch.getId()
                        + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            logger.severe("❌ Check batch release task failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 释放单个批次
     */
    private static void releaseBatch(CouponBatch batch) {
        Long batchId = batch.getId();
        Long activityId = batch.getActivityId();
        Integer stockCount = batch.getStockCount();

        logger.info("📦 Starting batch release - BatchID: " + batchId
            + ", ActivityID: " + activityId
            + ", Stock: " + stockCount);

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            // 1. 检查批次是否已经释放
            if (batch.getStatus() == 1) {
                logger.info("⚠️ Batch " + batchId + " already released");
                return;
            }

            // 2. 增加 Redis 库存
            String stockKey = COUPON_STOCK_PREFIX + activityId;
            String currentStock = RedisUtil.get(stockKey);

            if (currentStock == null) {
                logger.warning("⚠️ Redis stock key not found for activity " + activityId);
                return;
            }

            int newStock = Integer.parseInt(currentStock) + stockCount;
            RedisUtil.set(stockKey, String.valueOf(newStock));

            logger.info("✅ Redis stock updated - ActivityID: " + activityId
                + ", Old Stock: " + currentStock
                + ", Added: " + stockCount
                + ", New Stock: " + newStock);

            // 3. 更新批次状态为已释放
            couponDao.updateBatchReleased(conn, batchId, stockCount);

            logger.info("✅ Batch release completed - BatchID: " + batchId);

        } catch (Exception e) {
            logger.severe("❌ Release batch failed for batch " + batchId);
            e.printStackTrace();
            throw new RuntimeException("Release batch failed", e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 停止定时任务
     */
    public static void stop() {
        scheduler.shutdown();
        logger.info("🛑 Coupon batch release task stopped");
    }
}
