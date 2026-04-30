package com.xiaozhuo.test;

import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.entity.CouponActivity;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.RedisUtil;

import java.sql.Connection;

/**
 * 优惠券库存初始化测试
 */
public class CouponStockInitTest {

    public static void main(String[] args) {
        System.out.println("========== 优惠券库存初始化 ==========\n");

        CouponDao couponDao = new CouponDaoImpl();
        Connection conn = null;

        try {
            conn = ConnectionPool.getConnection();

            // 查询所有活动
            Long activityId = 1L; // 测试活动ID
            CouponActivity activity = couponDao.selectById(conn, activityId);

            if (activity == null) {
                System.out.println("❌ 活动不存在: ID=" + activityId);
                return;
            }

            System.out.println("活动信息:");
            System.out.println("  标题: " + activity.getTitle());
            System.out.println("  总库存: " + activity.getTotalStock());
            System.out.println("  剩余库存: " + activity.getRemainingStock());

            // 将库存同步到 Redis
            String stockKey = "coupon:stock:" + activityId;

            // 先删除旧数据
            RedisUtil.del(stockKey);

            // 设置新库存
            RedisUtil.set(stockKey, String.valueOf(activity.getRemainingStock()));

            System.out.println("\n✅ Redis 库存初始化成功!");
            System.out.println("  Key: " + stockKey);
            System.out.println("  Value: " + RedisUtil.get(stockKey));

        } catch (Exception e) {
            System.err.println("❌ 初始化失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }

        System.out.println("\n========== 初始化完成 ==========");
    }
}