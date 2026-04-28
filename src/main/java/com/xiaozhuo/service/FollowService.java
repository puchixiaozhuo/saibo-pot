package com.xiaozhuo.service;

import com.xiaozhuo.bean.vo.FollowVO;
import com.xiaozhuo.result.Result;

import java.util.List;
import java.util.Map;

public interface FollowService {
    /**
     * 关注用户
     * @param userId 当前用户ID
     * @param followId 被关注用户ID
     * @return 关注结果
     */
    Result<Void> followUser(Long userId, Long followId);

    /**
     * 取消关注用户
     * @param userId 当前用户ID
     * @param followId 被取消关注用户ID
     * @return 取消关注结果
     */
    Result<Void> unfollowUser(Long userId, Long followId);

    /**
     * 获取关注列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 关注列表
     */
    Result<List<FollowVO>> getFollowingList(Long userId, int pageNum, int pageSize);

    /**
     * 获取粉丝列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 粉丝列表
     */
    Result<List<FollowVO>> getFollowerList(Long userId, int pageNum, int pageSize);

    /**
     * 获取关注状态
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 关注状态
     */
    Result<Map<String, Object>> getFollowStatus(Long userId, Long targetUserId);

    /**
     * 获取关注数量
     * @param userId 用户ID
     * @return 关注数量（关注数、粉丝数）
     */
    Result<Map<String, Object>> getFollowCounts(Long userId);
}
