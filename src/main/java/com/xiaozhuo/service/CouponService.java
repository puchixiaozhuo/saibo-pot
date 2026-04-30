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
}