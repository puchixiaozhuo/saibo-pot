package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.service.CommentService;
import com.xiaozhuo.service.Impl.CommentServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/comment/*")
public class CommentServlet extends HttpServlet {

    private CommentService commentService = new CommentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            out.print(JSON.toJSONString(errorResult(400, "请求路径错误")));
            return;
        }

        try {
            if (pathInfo.startsWith("/video/")) {
                handleGetCommentsByVideo(req, pathInfo, out);
            } else if (pathInfo.startsWith("/user/")) {
                handleGetUserComments(req, pathInfo, out);
            } else {
                out.print(JSON.toJSONString(errorResult(404, "接口不存在")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print(JSON.toJSONString(errorResult(500, "服务器内部错误：" + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            out.print(JSON.toJSONString(errorResult(400, "请求路径错误")));
            return;
        }

        try {
            if (pathInfo.equals("/add")) {
                handleAddComment(req, userId, out);
            } else if (pathInfo.matches("/\\d+/like")) {
                Long commentId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/like")));
                handleLikeComment(userId, commentId, out);
            } else if (pathInfo.matches("/\\d+/unlike")) {
                Long commentId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/unlike")));
                handleUnlikeComment(userId, commentId, out);
            } else {
                out.print(JSON.toJSONString(errorResult(404, "接口不存在")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print(JSON.toJSONString(errorResult(500, "服务器内部错误：" + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            Long commentId = Long.parseLong(pathInfo.substring(1));
            handleDeleteComment(userId, commentId, out);
        } else {
            out.print(JSON.toJSONString(errorResult(400, "请求路径错误")));
        }
    }

    private void handleAddComment(HttpServletRequest req, Long userId, PrintWriter out) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line);
        }

        Map<String, Object> requestBody = JSON.parseObject(sb.toString(), Map.class);
        Long videoId = Long.valueOf(requestBody.get("videoId").toString());
        String content = (String) requestBody.get("content");
        Long parentId = requestBody.containsKey("parentId") ? Long.valueOf(requestBody.get("parentId").toString()) : 0L;

        Map<String, Object> result = commentService.addComment(userId, videoId, content, parentId);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetCommentsByVideo(HttpServletRequest req, String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            Long videoId = Long.parseLong(parts[2]);
            int pageNum = getIntParameter(req, "pageNum", 1);
            int pageSize = getIntParameter(req, "pageSize", 10);
            String sortBy = req.getParameter("sortBy");
            if (sortBy == null || sortBy.isEmpty()) {
                sortBy = "hot";
            }

            Map<String, Object> result = commentService.getCommentsByVideoId(videoId, pageNum, pageSize, sortBy);
            out.print(JSON.toJSONString(result));
        } catch (Exception e) {
            out.print(JSON.toJSONString(errorResult(400, "参数错误")));
        }
    }

    private void handleDeleteComment(Long userId, Long commentId, PrintWriter out) {
        Map<String, Object> result = commentService.deleteComment(userId, commentId);
        out.print(JSON.toJSONString(result));
    }

    private void handleLikeComment(Long userId, Long commentId, PrintWriter out) {
        Map<String, Object> result = commentService.likeComment(userId, commentId);
        out.print(JSON.toJSONString(result));
    }

    private void handleUnlikeComment(Long userId, Long commentId, PrintWriter out) {
        Map<String, Object> result = commentService.unlikeComment(userId, commentId);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetUserComments(HttpServletRequest req, String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            Long userId = Long.parseLong(parts[2]);
            int pageNum = getIntParameter(req, "pageNum", 1);
            int pageSize = getIntParameter(req, "pageSize", 10);

            Map<String, Object> result = commentService.getUserComments(userId, pageNum, pageSize);
            out.print(JSON.toJSONString(result));
        } catch (Exception e) {
            out.print(JSON.toJSONString(errorResult(400, "参数错误")));
        }
    }

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

    private Map<String, Object> errorResult(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        return result;
    }
}
