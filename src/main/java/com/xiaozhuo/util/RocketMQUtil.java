package com.xiaozhuo.util;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 工具类
 */
public class RocketMQUtil {

    private static final String NAME_SERVER = "127.0.0.1:9876"; // RocketMQ NameServer 地址
    private static final String PRODUCER_GROUP = "topview-producer-group";

    private static DefaultMQProducer producer;

    static {
        try {
            producer = new DefaultMQProducer(PRODUCER_GROUP);
            producer.setNamesrvAddr(NAME_SERVER);
            producer.setRetryTimesWhenSendFailed(3); // 发送失败重试3次
            producer.start();
            System.out.println("✅ RocketMQ Producer started");
        } catch (Exception e) {
            System.err.println("❌ RocketMQ Producer start failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送同步消息
     */
    public static void sendSyncMessage(String topic, String tag, String body) {
        try {
            Message msg = new Message(topic, tag, body.getBytes(StandardCharsets.UTF_8));
            producer.send(msg);
            System.out.println("✅ Message sent - Topic: " + topic + ", Tag: " + tag);
        } catch (Exception e) {
            System.err.println("❌ Send message failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送异步消息（带回调）
     */
    public static void sendAsyncMessage(String topic, String tag, String body) {
        try {
            Message msg = new Message(topic, tag, body.getBytes(StandardCharsets.UTF_8));
            producer.send(msg, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    System.out.println("✅ Async message sent successfully - MsgId: " + sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    System.err.println("❌ Async message send failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Send async message failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建消费者
     */
    public static DefaultMQPushConsumer createConsumer(String consumerGroup, String topic, String tag) {
        try {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
            consumer.setNamesrvAddr(NAME_SERVER);
            consumer.subscribe(topic, tag);
            return consumer;
        } catch (Exception e) {
            System.err.println("❌ Create consumer failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 关闭 Producer
     */
    public static void shutdown() {
        if (producer != null) {
            producer.shutdown();
            System.out.println("🛑 RocketMQ Producer shutdown");
        }
    }
}
