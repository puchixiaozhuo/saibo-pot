package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.service.FollowService;
import com.xiaozhuo.service.Impl.FollowServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/follow/*")
public class FollowServlet extends HttpServlet {

    private FollowService followService = new FollowServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            if (pathInfo.equals("/following")) {
                handleGetFollowingList(req, userId, out);
            } else if (pathInfo.equals("/followers")) {
                handleGetFollowerList(req, userId, out);
            } else if (pathInfo.matches("/counts")) {
                handleGetFollowCounts(userId, out);
            } else if (pathInfo.matches("/status/\\d+")) {
                Long targetUserId = Long.parseLong(pathInfo.substring(8));
                handleGetFollowStatus(userId, targetUserId, out);
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
            if (pathInfo.equals("/follow")) {
                handleFollowUser(req, userId, out);
            } else if (pathInfo.equals("/unfollow")) {
                handleUnfollowUser(req, userId, out);
            } else {
                out.print(JSON.toJSONString(errorResult(404, "接口不存在")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print(JSON.toJSONString(errorResult(500, "服务器内部错误：" + e.getMessage())));
        }
    }

    private void handleFollowUser(HttpServletRequest req, Long userId, PrintWriter out) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line);
        }

        Map<String, Object> requestBody = JSON.parseObject(sb.toString(), Map.class);
        Long followId = Long.valueOf(requestBody.get("followId").toString());

        Map<String, Object> result = followService.followUser(userId, followId);
        out.print(JSON.toJSONString(result));
    }

    private void handleUnfollowUser(HttpServletRequest req, Long userId, PrintWriter out) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line);
        }

        Map<String, Object> requestBody = JSON.parseObject(sb.toString(), Map.class);
        Long followId = Long.valueOf(requestBody.get("followId").toString());

        Map<String, Object> result = followService.unfollowUser(userId, followId);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetFollowingList(HttpServletRequest req, Long userId, PrintWriter out) {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Map<String, Object> result = followService.getFollowingList(userId, pageNum, pageSize);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetFollowerList(HttpServletRequest req, Long userId, PrintWriter out) {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Map<String, Object> result = followService.getFollowerList(userId, pageNum, pageSize);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetFollowStatus(Long userId, Long targetUserId, PrintWriter out) {
        Map<String, Object> result = followService.getFollowStatus(userId, targetUserId);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetFollowCounts(Long userId, PrintWriter out) {
        Map<String, Object> result = followService.getFollowCounts(userId);
        out.print(JSON.toJSONString(result));
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
