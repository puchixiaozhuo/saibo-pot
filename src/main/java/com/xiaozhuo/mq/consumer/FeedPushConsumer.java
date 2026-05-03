package com.xiaozhuo.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.constant.MQConstant;
import com.xiaozhuo.dao.FollowDao;
import com.xiaozhuo.dao.impl.FollowDaoImpl;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.RedisUtil;
import com.xiaozhuo.util.RocketMQUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Feed 流推送消费者
 */
public class FeedPushConsumer {

    private static final FollowDao followDao = new FollowDaoImpl();
    private static final String FEED_PUSH_PREFIX = "feed:push:";
    private static final int FEED_PUSH_MAX_SIZE = 1000;
    private static final int FEED_PUSH_EXPIRE_DAYS = 7;

    public static void start() {
        try {
            DefaultMQPushConsumer consumer = RocketMQUtil.createConsumer(
                MQConstant.CONSUMER_GROUP_FEED,
                MQConstant.TOPIC_FEED_PUSH,
                MQConstant.TAG_FEED_NEW_VIDEO
            );

            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    for (MessageExt msg : msgs) {
                        try {
                            String body = new String(msg.getBody(), "UTF-8");
                            Map<String, Object> message = JSON.parseObject(body, Map.class);

                            Long authorId = Long.parseLong(message.get("authorId").toString());
                            Long videoId = Long.parseLong(message.get("videoId").toString());
                            String publishTimeStr = message.get("publishTime").toString();

                            pushToFollowers(authorId, videoId, publishTimeStr);

                            System.out.println("✅ Feed pushed - VideoID: " + videoId + " from AuthorID: " + authorId);

                        } catch (Exception e) {
                            System.err.println("❌ Feed push failed: " + e.getMessage());
                            e.printStackTrace();
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER; // 重试
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            consumer.start();
            System.out.println("✅ FeedPushConsumer started");

        } catch (Exception e) {
            System.err.println("❌ FeedPushConsumer start failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void pushToFollowers(Long authorId, Long videoId, String publishTimeStr) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            List<Long> followerIds = followDao.getFollowerUserIds(conn, authorId);

            if (followerIds == null || followerIds.isEmpty()) {
                System.out.println("ℹ️ Author " + authorId + " has no followers");
                return;
            }

            long score = java.time.LocalDateTime.parse(publishTimeStr)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

            for (Long followerId : followerIds) {
                try {
                    String feedKey = FEED_PUSH_PREFIX + followerId;
                    RedisUtil.zadd(feedKey, score, String.valueOf(videoId));
                    RedisUtil.zremrangeByRank(feedKey, 0, -(FEED_PUSH_MAX_SIZE + 1));
                    RedisUtil.expire(feedKey, FEED_PUSH_EXPIRE_DAYS * 86400);
                } catch (Exception e) {
                    System.err.println("❌ Push to follower " + followerId + " failed: " + e.getMessage());
                }
            }

            System.out.println("✅ Pushed to " + followerIds.size() + " followers");

        } catch (Exception e) {
            System.err.println("❌ Push to followers failed: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }
}
