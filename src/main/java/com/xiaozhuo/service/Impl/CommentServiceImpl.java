package com.xiaozhuo.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.bean.vo.CommentVO;
import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.impl.CommentDaoImpl;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.entity.VideoComment;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CommentService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.RedisUtil;
import com.xiaozhuo.util.TransactionManager;

import java.sql.Connection;
import java.util.*;

/**
 * 评论业务实现类（优化版）
 * 使用声明式事务 + IoC 容器管理
 */
@Bean
public class CommentServiceImpl implements CommentService {

    private CommentDao commentDao = new CommentDaoImpl();
    private UserDao userDao = new UserDaoImpl();

    private static final String COMMENT_CACHE_PREFIX = "comment:video:";
    private static final int CACHE_EXPIRE_SECONDS = 300;

    /**
     * 添加评论（声明式事务）
     */
    @Override
    @Transactional
    public Result<Map<String, Object>> addComment(Long userId, Long videoId, String content, Long parentId) {
        if (userId == null || videoId == null) {
            throw new BusinessException(400, "用户ID和视频ID不能为空");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "评论内容不能为空");
        }

        if (content.length() > 500) {
            throw new BusinessException(400, "评论内容不能超过500字");
        }

        Connection conn = TransactionManager.getConnection();

        VideoComment comment = new VideoComment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setLikeCount(0L);

        int rows = commentDao.insert(conn, comment);
        if (rows > 0) {
            clearCommentCache(videoId);

            Map<String, Object> data = new HashMap<>();
            data.put("commentId", comment.getId());
            data.put("content", comment.getContent());
            data.put("createTime", comment.getCreateTime());

            return Result.success("评论成功", data);
        } else {
            throw new BusinessException(500, "评论失败");
        }
    }

    /**
     * 根据视频ID获取评论
     */
    @Override
    public Result<List<CommentVO>> getCommentsByVideoId(Long videoId, int pageNum, int pageSize, String sortBy) {
        if (videoId == null || videoId <= 0) {
            throw new BusinessException(400, "视频ID无效");
        }

        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        String cacheKey = COMMENT_CACHE_PREFIX + videoId + ":page:" + pageNum + ":sort:" + sortBy;
        String cachedData = RedisUtil.get(cacheKey);

        if (cachedData != null) {
            List<CommentVO> cachedList = JSON.parseArray(cachedData, CommentVO.class);
            return Result.success("查询成功（缓存）", cachedList);
        }

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            List<VideoComment> comments;
            if ("hot".equals(sortBy)) {
                comments = commentDao.selectByVideoIdOrderByHot(conn, videoId, pageNum, pageSize);
            } else {
                comments = commentDao.selectByVideoIdOrderByTime(conn, videoId, pageNum, pageSize);
            }

            List<CommentVO> commentVOList = convertToCommentVOList(conn, comments);

            RedisUtil.setex(cacheKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(commentVOList));

            return Result.success("查询成功", commentVOList);

        } catch (Exception e) {
            throw new BusinessException(500, "查询评论失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 删除评论（声明式事务）
     */
    @Override
    @Transactional
    public Result<Void> deleteComment(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            throw new BusinessException(400, "参数无效");
        }

        Connection conn = TransactionManager.getConnection();

        VideoComment comment = commentDao.selectById(conn, commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限删除，只能删除自己的评论");
        }

        int rows = commentDao.deleteById(conn, commentId);
        if (rows > 0) {
            clearCommentCache(comment.getVideoId());
            return Result.success();
        } else {
            throw new BusinessException(500, "删除失败");
        }
    }

    /**
     * 点赞评论（声明式事务）
     */
    @Override
    @Transactional
    public Result<Void> likeComment(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            throw new BusinessException(400, "参数无效");
        }

        String likeKey = "like:user:" + userId + ":comment:" + commentId;
        if (RedisUtil.exists(likeKey)) {
            throw new BusinessException(400, "已经点赞过");
        }

        Connection conn = TransactionManager.getConnection();

        int rows = commentDao.incrementLikeCount(conn, commentId);
        if (rows > 0) {
            RedisUtil.setex(likeKey, 86400 * 30, "1");

            VideoComment comment = commentDao.selectById(conn, commentId);
            if (comment != null) {
                clearCommentCache(comment.getVideoId());
            }

            return Result.success();
        } else {
            throw new BusinessException(500, "点赞失败");
        }
    }

    /**
     * 取消点赞评论（声明式事务）
     */
    @Override
    @Transactional
    public Result<Void> unlikeComment(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            throw new BusinessException(400, "参数无效");
        }

        String likeKey = "like:user:" + userId + ":comment:" + commentId;
        if (!RedisUtil.exists(likeKey)) {
            throw new BusinessException(400, "还未点赞");
        }

        Connection conn = TransactionManager.getConnection();

        int rows = commentDao.decrementLikeCount(conn, commentId);
        if (rows > 0) {
            RedisUtil.del(likeKey);

            VideoComment comment = commentDao.selectById(conn, commentId);
            if (comment != null) {
                clearCommentCache(comment.getVideoId());
            }

            return Result.success();
        } else {
            throw new BusinessException(500, "取消点赞失败");
        }
    }

    /**
     * 获取用户评论
     */
    @Override
    public Result<List<CommentVO>> getUserComments(Long userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            List<VideoComment> comments = commentDao.selectByUserId(conn, userId, pageNum, pageSize);
            List<CommentVO> commentVOList = convertToCommentVOList(conn, comments);

            return Result.success("查询成功", commentVOList);

        } catch (Exception e) {
            throw new BusinessException(500, "查询评论失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 将 VideoComment 列表转换为 CommentVO 列表
     */
    private List<CommentVO> convertToCommentVOList(Connection conn, List<VideoComment> comments) throws Exception {
        List<CommentVO> commentVOList = new ArrayList<>();

        for (VideoComment comment : comments) {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setVideoId(comment.getVideoId());
            vo.setUserId(comment.getUserId());
            vo.setParentId(comment.getParentId());
            vo.setContent(comment.getContent());
            vo.setLikeCount(comment.getLikeCount());
            vo.setCreateTime(comment.getCreateTime());

            User user = userDao.findById(conn, comment.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            commentVOList.add(vo);
        }

        return commentVOList;
    }

    /**
     * 清除评论缓存
     */
    private void clearCommentCache(Long videoId) {
        String pattern = COMMENT_CACHE_PREFIX + videoId + "*";
        Set<String> keys = RedisUtil.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            RedisUtil.del(keys.toArray(new String[0]));
        }
    }
}
