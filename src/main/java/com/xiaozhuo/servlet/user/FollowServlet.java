package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.bean.vo.FollowVO;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.FollowService;

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
 * 关注 Servlet（使用 IoC 容器 + 全局异常处理）
 */
@WebServlet("/api/follow/*")
public class FollowServlet extends HttpServlet {

    private FollowService followService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.followService = context.getBean(FollowService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    /**
     * GET 请求处理
     * - /api/follow/following - 获取关注列表
     * - /api/follow/followers - 获取粉丝列表
     * - /api/follow/counts - 获取关注数量
     * - /api/follow/status/{targetUserId} - 获取关注状态
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

            if (pathInfo.equals("/following")) {
                handleGetFollowingList(req, userId, resp);
            } else if (pathInfo.equals("/followers")) {
                handleGetFollowerList(req, userId, resp);
            } else if (pathInfo.equals("/counts")) {
                handleGetFollowCounts(userId, resp);
            } else if (pathInfo.matches("/status/\\d+")) {
                Long targetUserId = Long.parseLong(pathInfo.substring(8));
                handleGetFollowStatus(userId, targetUserId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * POST 请求处理
     * - /api/follow/follow - 关注用户
     * - /api/follow/unfollow - 取消关注
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

            if (pathInfo.equals("/follow")) {
                handleFollowUser(req, userId, resp);
            } else if (pathInfo.equals("/unfollow")) {
                handleUnfollowUser(req, userId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 关注用户
     */
    private void handleFollowUser(HttpServletRequest req, Long userId, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        Map<String, Object> requestBody = JSON.parseObject(sb.toString(), Map.class);

        if (!requestBody.containsKey("followId")) {
            throw new BusinessException(400, "被关注用户ID不能为空");
        }

        Long followId = Long.valueOf(requestBody.get("followId").toString());

        Result<Void> result = followService.followUser(userId, followId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 取消关注用户
     */
    private void handleUnfollowUser(HttpServletRequest req, Long userId, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        Map<String, Object> requestBody = JSON.parseObject(sb.toString(), Map.class);

        if (!requestBody.containsKey("followId")) {
            throw new BusinessException(400, "被取消关注用户ID不能为空");
        }

        Long followId = Long.valueOf(requestBody.get("followId").toString());

        Result<Void> result = followService.unfollowUser(userId, followId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 获取关注列表
     */
    private void handleGetFollowingList(HttpServletRequest req, Long userId, HttpServletResponse resp) throws IOException {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result<List<FollowVO>> result = followService.getFollowingList(userId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 获取粉丝列表
     */
    private void handleGetFollowerList(HttpServletRequest req, Long userId, HttpServletResponse resp) throws IOException {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result<List<FollowVO>> result = followService.getFollowerList(userId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 获取关注状态
     */
    private void handleGetFollowStatus(Long userId, Long targetUserId, HttpServletResponse resp) throws IOException {
        Result<Map<String, Object>> result = followService.getFollowStatus(userId, targetUserId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 获取关注数量
     */
    private void handleGetFollowCounts(Long userId, HttpServletResponse resp) throws IOException {
        Result<Map<String, Object>> result = followService.getFollowCounts(userId);
        resp.getWriter().write(JSON.toJSONString(result));
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
