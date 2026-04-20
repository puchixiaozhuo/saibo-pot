package com.xiaozhuo.service;

import java.util.Map;

public interface CommentService {
    /**
     * 添加评论
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param content 评论内容
     * @param parentId 父评论ID
     * @return 添加结果
     */
    Map<String, Object> addComment(Long userId, Long videoId, String content, Long parentId);
    /**
     * 根据视频ID获取评论
     * @param videoId 视频ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param sortBy 排序方式
     * @return 评论列表
     */
    Map<String, Object> getCommentsByVideoId(Long videoId, int pageNum, int pageSize, String sortBy);
    /**
     * 删除评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 删除结果
     */
    Map<String, Object> deleteComment(Long userId, Long commentId);
    /**
     * 点赞评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 点赞结果
     */
    Map<String, Object> likeComment(Long userId, Long commentId);
    /**
     * 取消点赞评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 取消点赞结果
     */
    Map<String, Object> unlikeComment(Long userId, Long commentId);
    /**
     * 获取用户评论
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 用户评论列表
     */
    Map<String, Object> getUserComments(Long userId, int pageNum, int pageSize);
}

