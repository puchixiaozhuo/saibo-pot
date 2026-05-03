package com.xiaozhuo.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaozhuo.bean.dto.CouponGrabMessage;
import com.xiaozhuo.constant.MQConstant;
import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.entity.CouponUser;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.RedisUtil;
import com.xiaozhuo.util.RocketMQUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 优惠券抢购消费者 - 削峰处理
 */
public class CouponGrabConsumer {

    private static final CouponDao couponDao = new CouponDaoImpl();
    private static final String COUPON_STOCK_PREFIX = "coupon:stock:";

    public static void start() {
        try {
            DefaultMQPushConsumer consumer = RocketMQUtil.createConsumer(
                MQConstant.CONSUMER_GROUP_GRAB,
                MQConstant.TOPIC_COUPON_GRAB,
                MQConstant.TAG_GRAB_REQUEST
            );

            // 设置消费者线程数（控制并发处理能力）
            consumer.setConsumeThreadMin(5);
            consumer.setConsumeThreadMax(10);

            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    for (MessageExt msg : msgs) {
                        try {
                            String body = new String(msg.getBody(), "UTF-8");

                            System.out.println("🔥🔥🔥 NEW CODE IS RUNNING! Body: " + body);

                            // 🔥 修复：直接解析为 JSONObject，避开 DTO 类加载问题
                            JSONObject json = JSON.parseObject(body);
                            Long userId = json.getLong("userId");
                            Long activityId = json.getLong("activityId");
                            String requestId = json.getString("requestId");

                            System.out.println("📨 Processing grab request - UserID: " + userId
                                    + ", ActivityID: " + activityId
                                    + ", RequestID: " + requestId);

                            processGrabRequest(userId, activityId, requestId);

                        } catch (Exception e) {
                            System.err.println("❌ Process grab request failed: " + e.getMessage());
                            e.printStackTrace();
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            consumer.start();
            System.out.println("✅ CouponGrabConsumer started (Threads: 5-10)");

        } catch (Exception e) {
            System.err.println("❌ CouponGrabConsumer start failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processGrabRequest(Long userId, Long activityId, String requestId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            // 1. 检查是否已领取
            CouponUser existingCoupon = couponDao.selectByUserIdAndActivityId(conn, userId, activityId);
            if (existingCoupon != null) {
                return;
            }

            // 2. 生成优惠券
            String couponCode = generateCouponCode();
            CouponUser couponUser = new CouponUser();
            couponUser.setActivityId(activityId);
            couponUser.setUserId(userId);
            couponUser.setCouponCode(couponCode);
            couponUser.setStatus(0);
            couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
            couponUser.setCreateTime(LocalDateTime.now());

            // 3. 插入数据库
            couponDao.insertCouponUser(conn, couponUser);

            // 4. 🔥 修复：使用 DAO 层的增量扣减方法，确保数据库库存同步减少
            couponDao.decrementStock(conn, activityId, 1);

            System.out.println("✅ Coupon generated & DB stock decremented - UserID: " + userId);

        } catch (Exception e) {
            System.err.println("❌ Process grab request failed for UserID: " + userId);
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 异步同步库存到数据库
     */
    private static void syncStockToDatabase(Long activityId) {
        try {
            String stockKey = COUPON_STOCK_PREFIX + activityId;
            String cachedStock = RedisUtil.get(stockKey);

            if (cachedStock != null) {
                Connection conn = ConnectionPool.getConnection();
                try {
                    String sql = "UPDATE coupon_activity SET remaining_stock = ? WHERE id = ?";
                    java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, Integer.parseInt(cachedStock));
                    ps.setLong(2, activityId);
                    ps.executeUpdate();
                    ps.close();

                    System.out.println("✅ Stock synced to DB - ActivityID: " + activityId
                        + ", Stock: " + cachedStock);
                } finally {
                    ConnectionPool.returnConnection(conn);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to sync stock: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成优惠券码
     */
    private static String generateCouponCode() {
        String timestamp = LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString()
            .replace("-", "").substring(0, 8).toUpperCase();
        return "CPN" + timestamp + random;
    }
}
