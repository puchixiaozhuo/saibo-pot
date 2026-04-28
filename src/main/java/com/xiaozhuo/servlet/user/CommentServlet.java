package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.bean.vo.CommentVO;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CommentService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 评论 Servlet（使用 IoC 容器 + 全局异常处理）
 */
@WebServlet("/api/comment/*")
public class CommentServlet extends HttpServlet {

    private CommentService commentService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.commentService = context.getBean(CommentService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    /**
     * GET 请求处理
     * - /api/comment/video/{videoId} - 获取视频评论列表
     * - /api/comment/user/{userId} - 获取用户评论列表
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.startsWith("/video/")) {
                handleGetCommentsByVideo(req, pathInfo, resp);
            } else if (pathInfo.startsWith("/user/")) {
                handleGetUserComments(req, pathInfo, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * POST 请求处理
     * - /api/comment/add - 添加评论
     * - /api/comment/{commentId}/like - 点赞评论
     * - /api/comment/{commentId}/unlike - 取消点赞
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "未登录，请先登录");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.equals("/add")) {
                handleAddComment(req, userId, resp);
            } else if (pathInfo.matches("/\\d+/like")) {
                Long commentId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/like")));
                handleLikeComment(userId, commentId, resp);
            } else if (pathInfo.matches("/\\d+/unlike")) {
                Long commentId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/unlike")));
                handleUnlikeComment(userId, commentId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * DELETE 请求处理
     * - /api/comment/{commentId} - 删除评论
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "未登录，请先登录");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long commentId = Long.parseLong(pathInfo.substring(1));
                handleDeleteComment(userId, commentId, resp);
            } else {
                throw new BusinessException(400, "请求路径错误");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 添加评论
     */
    private void handleAddComment(HttpServletRequest req, Long userId, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        Map<String, Object> requestBody = JSON.parseObject(sb.toString(), Map.class);

        if (!requestBody.containsKey("videoId") || !requestBody.containsKey("content")) {
            throw new BusinessException(400, "视频ID和评论内容不能为空");
        }

        Long videoId = Long.valueOf(requestBody.get("videoId").toString());
        String content = (String) requestBody.get("content");
        Long parentId = requestBody.containsKey("parentId") ? Long.valueOf(requestBody.get("parentId").toString()) : 0L;

        Result<Map<String, Object>> result = commentService.addComment(userId, videoId, content, parentId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 获取视频评论列表
     */
    private void handleGetCommentsByVideo(HttpServletRequest req, String pathInfo, HttpServletResponse resp) throws IOException {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length < 3) {
                throw new BusinessException(400, "视频ID不能为空");
            }

            Long videoId = Long.parseLong(parts[2]);
            int pageNum = getIntParameter(req, "pageNum", 1);
            int pageSize = getIntParameter(req, "pageSize", 10);
            String sortBy = req.getParameter("sortBy");
            if (sortBy == null || sortBy.isEmpty()) {
                sortBy = "hot";
            }

            Result<List<CommentVO>> result = commentService.getCommentsByVideoId(videoId, pageNum, pageSize, sortBy);
            resp.getWriter().write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "视频ID格式错误");
        }
    }

    /**
     * 删除评论
     */
    private void handleDeleteComment(Long userId, Long commentId, HttpServletResponse resp) throws IOException {
        Result<Void> result = commentService.deleteComment(userId, commentId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 点赞评论
     */
    private void handleLikeComment(Long userId, Long commentId, HttpServletResponse resp) throws IOException {
        Result<Void> result = commentService.likeComment(userId, commentId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 取消点赞评论
     */
    private void handleUnlikeComment(Long userId, Long commentId, HttpServletResponse resp) throws IOException {
        Result<Void> result = commentService.unlikeComment(userId, commentId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 获取用户评论列表
     */
    private void handleGetUserComments(HttpServletRequest req, String pathInfo, HttpServletResponse resp) throws IOException {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length < 3) {
                throw new BusinessException(400, "用户ID不能为空");
            }

            Long userId = Long.parseLong(parts[2]);
            int pageNum = getIntParameter(req, "pageNum", 1);
            int pageSize = getIntParameter(req, "pageSize", 10);

            Result<List<CommentVO>> result = commentService.getUserComments(userId, pageNum, pageSize);
            resp.getWriter().write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "用户ID格式错误");
        }
    }

    /**
     * 获取整数参数
     */
    private int getIntParameter(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
