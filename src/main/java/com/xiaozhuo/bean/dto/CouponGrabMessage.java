package com.xiaozhuo.bean.dto;

import java.io.Serializable;

/**
 * 优惠券抢购消息 DTO
 */
public class CouponGrabMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 活动ID */
    private Long activityId;

    /** 请求时间戳 */
    private Long requestTime;

    /** 请求ID（用于去重） */
    private String requestId;

    public CouponGrabMessage() {
    }

    public CouponGrabMessage(Long userId, Long activityId, Long requestTime, String requestId) {
        this.userId = userId;
        this.activityId = activityId;
        this.requestTime = requestTime;
        this.requestId = requestId;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Long requestTime) {
        this.requestTime = requestTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "CouponGrabMessage{" +
                "userId=" + userId +
                ", activityId=" + activityId +
                ", requestTime=" + requestTime +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
