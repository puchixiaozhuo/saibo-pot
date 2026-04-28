package com.xiaozhuo.service;

import com.xiaozhuo.bean.vo.CommentVO;
import com.xiaozhuo.result.Result;

import java.util.List;
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
    Result<Map<String, Object>> addComment(Long userId, Long videoId, String content, Long parentId);

    /**
     * 根据视频ID获取评论
     * @param videoId 视频ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param sortBy 排序方式
     * @return 评论列表
     */
    Result<List<CommentVO>> getCommentsByVideoId(Long videoId, int pageNum, int pageSize, String sortBy);

    /**
     * 删除评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 删除结果
     */
    Result<Void> deleteComment(Long userId, Long commentId);

    /**
     * 点赞评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 点赞结果
     */
    Result<Void> likeComment(Long userId, Long commentId);

    /**
     * 取消点赞评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 取消点赞结果
     */
    Result<Void> unlikeComment(Long userId, Long commentId);

    /**
     * 获取用户评论
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 用户评论列表
     */
    Result<List<CommentVO>> getUserComments(Long userId, int pageNum, int pageSize);
}
