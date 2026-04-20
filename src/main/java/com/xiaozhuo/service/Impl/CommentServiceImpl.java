package com.xiaozhuo.service.Impl;

import com.xiaozhuo.bean.vo.CommentVO;
import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.impl.CommentDaoImpl;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.entity.VideoComment;
import com.xiaozhuo.service.CommentService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.RedisUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CommentServiceImpl implements CommentService {

    private CommentDao commentDao = new CommentDaoImpl();
    private UserDao userDao = new UserDaoImpl();

    @Override
    public Map<String, Object> addComment(Long userId, Long videoId, String content, Long parentId) {
        Map<String, Object> result = new HashMap<>();

        if (content == null || content.trim().isEmpty()) {
            result.put("code", 400);
            result.put("message", "评论内容不能为空");
            return result;
        }

        if (content.length() > 500) {
            result.put("code", 400);
            result.put("message", "评论内容不能超过500字");
            return result;
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            conn.setAutoCommit(false);

            VideoComment comment = new VideoComment();
            comment.setVideoId(videoId);
            comment.setUserId(userId);
            comment.setContent(content.trim());
            comment.setParentId(parentId != null ? parentId : 0L);

            int rows = commentDao.insert(conn, comment);
            if (rows > 0) {
                conn.commit();

                String cacheKey = "comment:video:" + videoId;
                RedisUtil.del(cacheKey);

                result.put("code", 200);
                result.put("message", "评论成功");
                result.put("data", comment);
            } else {
                conn.rollback();
                result.put("code", 500);
                result.put("message", "评论失败");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            result.put("code", 500);
            result.put("message", "评论异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getCommentsByVideoId(Long videoId, int pageNum, int pageSize, String sortBy) {
        Map<String, Object> result = new HashMap<>();

        String cacheKey = "comment:video:" + videoId + ":page:" + pageNum + ":sort:" + sortBy;
        String cachedData = RedisUtil.get(cacheKey);

        if (cachedData != null) {
            result.put("code", 200);
            result.put("message", "查询成功（缓存）");
            result.put("data", com.alibaba.fastjson.JSON.parseObject(cachedData, Map.class));
            result.put("fromCache", true);
            return result;
        }

        Connection conn = null;
        try { conn = JDBCUtil.getConnection();

            List<VideoComment> comments;
            if ("hot".equals(sortBy)) {
                comments = commentDao.selectByVideoIdOrderByHot(conn, videoId, pageNum, pageSize);
            } else {
                comments = commentDao.selectByVideoIdOrderByTime(conn, videoId, pageNum, pageSize);
            }

            int total = commentDao.countByVideoId(conn, videoId);

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

            Map<String, Object> data = new HashMap<>();
            data.put("comments", commentVOList);
            data.put("total", total);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            data.put("totalPages", (total + pageSize - 1) / pageSize);

            RedisUtil.setex(cacheKey, 300, com.alibaba.fastjson.JSON.toJSONString(data));

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", data);
            result.put("fromCache", false);

        } catch (SQLException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> deleteComment(Long userId, Long commentId) {
        Map<String, Object> result = new HashMap<>();

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            VideoComment comment = commentDao.selectById(conn, commentId);
            if (comment == null) {
                result.put("code", 404);
                result.put("message", "评论不存在");
                return result;
            }

            if (!comment.getUserId().equals(userId)) {
                result.put("code", 403);
                result.put("message", "无权限删除，只能删除自己的评论");
                return result;
            }

            int rows = commentDao.deleteById(conn, commentId);
            if (rows > 0) {
                String cacheKey = "comment:video:" + comment.getVideoId();
                RedisUtil.del(cacheKey);

                result.put("code", 200);
                result.put("message", "删除成功");
            } else {
                result.put("code", 500);
                result.put("message", "删除失败");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "删除异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> likeComment(Long userId, Long commentId) {
        Map<String, Object> result = new HashMap<>();

        String likeKey = "like:user:" + userId + ":comment:" + commentId;
        if (RedisUtil.exists(likeKey)) {
            result.put("code", 400);
            result.put("message", "已经点赞过");
            return result;
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            int rows = commentDao.incrementLikeCount(conn, commentId);
            if (rows > 0) {
                RedisUtil.setex(likeKey, 86400 * 30, "1");

                VideoComment comment = commentDao.selectById(conn, commentId);
                if (comment != null) {
                    String cacheKey = "comment:video:" + comment.getVideoId();
                    RedisUtil.del(cacheKey);
                }

                result.put("code", 200);
                result.put("message", "点赞成功");
            } else {
                result.put("code", 500);
                result.put("message", "点赞失败");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "点赞异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> unlikeComment(Long userId, Long commentId) {
        Map<String, Object> result = new HashMap<>();

        String likeKey = "like:user:" + userId + ":comment:" + commentId;
        if (!RedisUtil.exists(likeKey)) {
            result.put("code", 400);
            result.put("message", "还未点赞");
            return result;
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            int rows = commentDao.decrementLikeCount(conn, commentId);
            if (rows > 0) {
                RedisUtil.del(likeKey);

                VideoComment comment = commentDao.selectById(conn, commentId);
                if (comment != null) {
                    String cacheKey = "comment:video:" + comment.getVideoId();
                    RedisUtil.del(cacheKey);
                }

                result.put("code", 200);
                result.put("message", "取消点赞成功");
            } else {
                result.put("code", 500);
                result.put("message", "取消点赞失败");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "取消点赞异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getUserComments(Long userId, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            List<VideoComment> comments = commentDao.selectByUserId(conn, userId, pageNum, pageSize);

            List<CommentVO> commentVOList = comments.stream().map(comment -> {
                CommentVO vo = new CommentVO();
                vo.setId(comment.getId());
                vo.setVideoId(comment.getVideoId());
                vo.setUserId(comment.getUserId());
                vo.setParentId(comment.getParentId());
                vo.setContent(comment.getContent());
                vo.setLikeCount(comment.getLikeCount());
                vo.setCreateTime(comment.getCreateTime());
                return vo;
            }).collect(Collectors.toList());

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", commentVOList);
            result.put("total", commentVOList.size());
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return result;
    }
}
