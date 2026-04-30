package com.xiaozhuo.dao;

import com.xiaozhuo.entity.CouponActivity;
import com.xiaozhuo.entity.CouponUser;

import java.sql.Connection;
import java.util.List;

public interface CouponDao {
    /**
     * 根据ID查询活动
     */
    CouponActivity selectById(Connection conn, Long id);

    /**
     * 使用乐观锁扣减库存
     * @return 影响行数，0表示扣减失败（库存不足或版本冲突）
     */
    int decrementStockWithOptimisticLock(Connection conn, Long activityId, Integer version);

    /**
     * 插入用户优惠券记录
     */
    int insertCouponUser(Connection conn, CouponUser couponUser);

    /**
     * 检查用户是否已领取
     */
    CouponUser selectByUserIdAndActivityId(Connection conn, Long userId, Long activityId);

    /**
     * 查询用户的优惠券列表
     */
    List<CouponUser> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize);

    /**
     * 统计用户优惠券数量
     */
    long countByUserId(Connection conn, Long userId);
}