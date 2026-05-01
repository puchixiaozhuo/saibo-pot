package com.xiaozhuo.service;

import com.xiaozhuo.entity.CouponUser;
import com.xiaozhuo.result.Result;

import java.util.List;
import java.util.Map;

public interface CouponService {

    /**
     * 抢购优惠券（普通模式）
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 抢购结果
     */
    Result<Map<String, Object>> grabCoupon(Long userId, Long activityId);

    /**
     * 查询我的优惠券
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 优惠券列表
     */
    Result<List<CouponUser>> getMyCoupons(Long userId, int pageNum, int pageSize);

    /**
     * 查询活动详情
     * @param activityId 活动ID
     * @return 活动信息
     */
    Result<Map<String, Object>> getActivityDetail(Long activityId);

    /**
     * 初始化活动库存到 Redis
     * @param activityId 活动ID
     * @return 初始化结果
     */
    Result<Boolean> initStockToRedis(Long activityId);

    /**
     * 释放待处理批次
     * @return 释放结果
      */
    Result<Integer> releasePendingBatches();

    /**
     * 获取活动批次列表
     * @param activityId 活动ID
     * @return 批次列表
     */
    Result<List<Map<String, Object>>> getActivityBatches(Long activityId);
    /**
     * 预约抢券（方案2）
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 预约结果
     */
    Result<Boolean> reserveCoupon(Long userId, Long activityId);

    /**
     * 执行抽签（方案2 - 管理员/定时任务触发）
     * @param activityId 活动ID
     * @return 抽签结果（中签人数）
     */
    Result<Integer> executeLottery(Long activityId);

    /**
     * 查询我的预约状态
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 预约状态
     */
    Result<Map<String, Object>> getMyReservationStatus(Long userId, Long activityId);

    /**
     * 上报观看进度
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param activityId 活动ID
     * @param watchedSeconds 本次观看时长（秒）
     * @return 上报结果
     */
    Result<Boolean> reportWatchProgress(Long userId, Long videoId, Long activityId, Integer watchedSeconds);

    /**
     * 检查是否已解锁优惠券
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param activityId 活动ID
     * @return 解锁状态
     */
    Result<Map<String, Object>> checkUnlockStatus(Long userId, Long videoId, Long activityId);

    /**
     * 领取观看解锁优惠券
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 领取结果
     */
    Result<Map<String, Object>> claimWatchUnlockCoupon(Long userId, Long activityId);
}