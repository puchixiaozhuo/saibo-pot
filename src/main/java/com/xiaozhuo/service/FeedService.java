package com.xiaozhuo.service;

import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;

import java.util.List;

/**
 * Feed流服务接口
 */
public interface FeedService {

    /**
     * 拉取用户的Feed流（关注用户的视频）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> pullFeed(Long userId, int pageNum, int pageSize);

    /**
     * 基于游标拉取Feed流（加分项）
     * @param userId 用户ID
     * @param cursor 游标（最后一条视频的时间戳或ID）
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> pullFeedWithCursor(Long userId, String cursor, int pageSize);

    /**
     * Push模式：当博主发布视频时，推送到所有粉丝的Feed流
     * @param authorId 博主ID
     * @param videoId 视频ID
     * @param publishTime 发布时间
     * @return 推送结果
     */
    Result<Integer> pushFeedToFollowers(Long authorId, Long videoId, java.time.LocalDateTime publishTime);

    /**
     * 拉取Push模式的Feed流（从Redis读取）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> pullPushFeed(Long userId, int pageNum, int pageSize);

    /**
     * 获取未读Feed数量（Push模式 - 从Redis读取）
     * @param userId 用户ID
     * @param lastReadTime 最后阅读时间
     * @return 未读数量
     */
    Result<Long> getUnreadFeedCount(Long userId, java.time.LocalDateTime lastReadTime);

    /**
     * 获取未读Feed数量（Pull模式 - 从数据库读取）
     * @param userId 用户ID
     * @param lastReadTime 最后阅读时间
     * @return 未读数量
     */
    Result<Long> getUnreadFeedCountFromDB(Long userId, java.time.LocalDateTime lastReadTime);
}