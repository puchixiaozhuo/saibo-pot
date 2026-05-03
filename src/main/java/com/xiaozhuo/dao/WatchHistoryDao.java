package com.xiaozhuo.dao;

import com.xiaozhuo.entity.UserWatchHistory;

import java.sql.Connection;
import java.util.List;

public interface WatchHistoryDao {
    /**
     * 插入或更新观看记录
     * @param conn 数据库连接
     * @param history 观看记录
     * @return 影响行数
     */
    int upsert(Connection conn, UserWatchHistory history);

    /**
     * 查询用户的观看历史（分页）
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 观看历史列表
     */
    List<UserWatchHistory> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize);

    /**
     * 统计用户观看历史数量
     * @param conn 数据库连接
     * @param userId 用户ID
     * @return 总数
     */
    long countByUserId(Connection conn, Long userId);

    /**
     * 根据用户ID和视频ID查询观看记录
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 观看记录
     */
    UserWatchHistory selectByUserIdAndVideoId(Connection conn, Long userId, Long videoId);

    /**
     * 删除观看记录
     * @param conn 数据库连接
     * @param historyId 历史记录ID
     * @return 影响行数
     */
    int deleteById(Connection conn, Long historyId);

    /**
     * 清空用户的观看历史
     * @param conn 数据库连接
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(Connection conn, Long userId);
}