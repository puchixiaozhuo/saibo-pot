package com.xiaozhuo.service;

import java.util.Map;

public interface FollowService {
    /**
     * 关注用户
     * @param userId
     * @param followId
     * @return
     */
    Map<String, Object> followUser(Long userId, Long followId);
    /**
     * 取消关注用户
     * @param userId
     * @param followId
     * @return
     */
    Map<String, Object> unfollowUser(Long userId, Long followId);
    /**
     * 获取关注列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    Map<String, Object> getFollowingList(Long userId, int pageNum, int pageSize);
    /**
     * 获取粉丝列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    Map<String, Object> getFollowerList(Long userId, int pageNum, int pageSize);
    /**
     * 获取关注状态
     * @param userId
     * @param targetUserId
     * @return
     */
    Map<String, Object> getFollowStatus(Long userId, Long targetUserId);
    /**
     * 获取关注数量
     * @param userId
     * @return
     */
    Map<String, Object> getFollowCounts(Long userId);
}