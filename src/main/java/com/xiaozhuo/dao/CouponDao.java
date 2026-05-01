package com.xiaozhuo.dao;

import com.xiaozhuo.entity.*;

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
     * 检查用户是否已领取优惠券
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

    /**
     * 查询待释放的批次
     * @param conn 数据库连接
     * @return 批次列表
     */
    List<CouponBatch> selectPendingBatches(Connection conn);

    /**
     * 更新批次状态为已释放
     * @param conn 数据库连接
     * @param batchId 批次ID
     * @param releasedStock 已释放数量
     * @return 影响行数
     */
    int updateBatchReleased(Connection conn, Long batchId, Integer releasedStock);

    /**
     * 根据活动ID查询所有批次
     * @param conn 数据库连接
     * @param activityId 活动ID
     * @return 批次列表
     */
    List<CouponBatch> selectByActivityId(Connection conn, Long activityId);
    /**
     * 插入预约记录
     * @param conn 数据库连接
     * @param reservation 预约记录
     * @return 影响行数
     */
    int insertReservation(Connection conn, CouponReservation reservation);

    /**
     * 根据用户ID和活动ID查询预约记录
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 预约记录
     */
    CouponReservation selectReservationByUserIdAndActivityId(Connection conn, Long userId, Long activityId);

    /**
     * 根据活动ID查询所有预约用户
     * @param conn 数据库连接
     * @param activityId 活动ID
     * @return 预约用户列表
     */
    List<CouponReservation> selectReservationsByActivityId(Connection conn, Long activityId);

    /**
     * 更新预约状态
     * @param conn 数据库连接
     * @param reservationId 预约ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateReservationStatus(Connection conn, Long reservationId, Integer status);

    /**
     * 统计活动预约人数
     * @param conn 数据库连接
     * @param activityId 活动ID
     * @return 预约人数
     */
    int countReservationsByActivityId(Connection conn, Long activityId);
    /**
     * 扣减库存（不使用乐观锁）
     * @param conn 数据库连接
     * @param activityId 活动ID
     * @param amount 扣减数量
     * @return 影响行数
     */
    int decrementStock(Connection conn, Long activityId, Integer amount);

    /**
     * 查询所有已结束但未抽签的活动
     * @param conn 数据库连接
     * @return 活动列表
     */
    List<CouponActivity> selectEndedActivities(Connection conn);
    /**
     * 插入或更新观看记录
     * @param conn 数据库连接
     * @param record 观看记录
     * @return 影响行数
     */
    int upsertWatchRecord(Connection conn, UserVideoWatchRecord record);

    /**
     * 查询用户观看记录
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param activityId 活动ID
     * @return 观看记录
     */
    UserVideoWatchRecord selectWatchRecord(Connection conn, Long userId, Long videoId, Long activityId);

    /**
     * 更新观看时长
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param activityId 活动ID
     * @param additionalSeconds 增加的观看时长
     * @return 影响行数
     */
    int updateWatchedSeconds(Connection conn, Long userId, Long videoId, Long activityId, Integer additionalSeconds);

    /**
     * 标记为已解锁
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param activityId 活动ID
     * @return 影响行数
     */
    int markAsUnlocked(Connection conn, Long userId, Long videoId, Long activityId);
}