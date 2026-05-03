package com.xiaozhuo.service;

import com.xiaozhuo.entity.UserWatchHistory;
import com.xiaozhuo.result.Result;

import java.util.List;
import java.util.Map;

public interface WatchHistoryService {

    /**
     * 上报观看进度
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param progress 观看进度（秒）
     * @param duration 本次观看时长（秒）
     * @return 上报结果
     */
    Result<Boolean> reportWatchProgress(Long userId, Long videoId, Integer progress, Integer duration);

    /**
     * 查询观看历史（分页）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 观看历史列表（含视频信息）
     */
    Result<List<Map<String, Object>>> getWatchHistory(Long userId, int pageNum, int pageSize);

    /**
     * 查询单个视频的观看记录
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 观看记录
     */
    Result<UserWatchHistory> getWatchRecord(Long userId, Long videoId);

    /**
     * 删除观看记录
     * @param userId 用户ID
     * @param historyId 历史记录ID
     * @return 删除结果
     */
    Result<Boolean> deleteWatchRecord(Long userId, Long historyId);

    /**
     * 清空观看历史
     * @param userId 用户ID
     * @return 清空结果
     */
    Result<Boolean> clearWatchHistory(Long userId);
}