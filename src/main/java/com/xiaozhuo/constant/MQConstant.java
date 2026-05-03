package com.xiaozhuo.constant;

/**
 * RocketMQ 消息常量
 */
public class MQConstant {

    // ==================== Topic 定义 ====================

    /** Feed 流推送 Topic */
    public static final String TOPIC_FEED_PUSH = "TOPIC_FEED_PUSH";

    /** 优惠券抢购 Topic */
    public static final String TOPIC_COUPON_GRAB = "TOPIC_COUPON_GRAB";

    /** 库存同步 Topic */
    public static final String TOPIC_STOCK_SYNC = "TOPIC_STOCK_SYNC";

    /** 活动状态变更 Topic */
    public static final String TOPIC_ACTIVITY_EVENT = "TOPIC_ACTIVITY_EVENT";

    // ==================== Tag 定义 ====================

    /** Feed 推送标签 */
    public static final String TAG_FEED_NEW_VIDEO = "NEW_VIDEO";

    /** 抢购请求标签 */
    public static final String TAG_GRAB_REQUEST = "GRAB_REQUEST";

    /** 库存扣减标签 */
    public static final String TAG_STOCK_DECREASE = "STOCK_DECREASE";

    /** 活动开始标签 */
    public static final String TAG_ACTIVITY_START = "ACTIVITY_START";

    /** 活动结束标签 */
    public static final String TAG_ACTIVITY_END = "ACTIVITY_END";

    // ==================== Consumer Group 定义 ====================

    /** Feed 推送消费者组 */
    public static final String CONSUMER_GROUP_FEED = "consumer-group-feed-push";

    /** 抢购处理消费者组 */
    public static final String CONSUMER_GROUP_GRAB = "consumer-group-coupon-grab";

    /** 库存同步消费者组 */
    public static final String CONSUMER_GROUP_STOCK = "consumer-group-stock-sync";
}
