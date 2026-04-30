package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.FeedService;
import com.xiaozhuo.handler.GlobalExceptionHandler;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Feed流Servlet - Pull模式
 */
@WebServlet("/api/feed/*")
public class FeedServlet extends HttpServlet {

    private FeedService feedService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.feedService = context.getBean(FeedService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

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


            if (pathInfo.equals("/pull")) {
                handlePullFeed(req, resp, userId);
            } else if (pathInfo.equals("/pull-cursor")) {
                handlePullFeedWithCursor(req, resp, userId);
            } else if (pathInfo.equals("/pull-push")) {
                handlePullPushFeed(req, resp, userId);
            } else if (pathInfo.equals("/unread-count")) {
                handleGetUnreadCount(req, resp, userId);
            } else if (pathInfo.equals("/unread-count-db")) {
                handleGetUnreadCountFromDB(req, resp, userId);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }


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

            if (pathInfo.matches("/\\d+/push")) {
                Long videoId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/push")));
                handlePushFeed(userId, videoId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 处理普通分页拉取Feed流
     */
    private void handlePullFeed(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result result = feedService.pullFeed(userId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 处理基于游标的Feed流拉取
     */
    private void handlePullFeedWithCursor(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        String cursor = req.getParameter("cursor");
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result result = feedService.pullFeedWithCursor(userId, cursor, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }


    /**
     * 处理Push模式的Feed流拉取
     */
    private void handlePullPushFeed(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result result = feedService.pullPushFeed(userId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 处理获取未读Feed数量（Push模式 - Redis）
     */
    private void handleGetUnreadCount(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        String lastReadTimeStr = req.getParameter("lastReadTime");
        java.time.LocalDateTime lastReadTime = null;

        if (lastReadTimeStr != null && !lastReadTimeStr.isEmpty()) {
            lastReadTime = java.time.LocalDateTime.parse(lastReadTimeStr);
        }

        Result result = feedService.getUnreadFeedCount(userId, lastReadTime);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 处理获取未读Feed数量（Pull模式 - 数据库）
     */
    private void handleGetUnreadCountFromDB(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        String lastReadTimeStr = req.getParameter("lastReadTime");
        java.time.LocalDateTime lastReadTime = null;

        if (lastReadTimeStr != null && !lastReadTimeStr.isEmpty()) {
            lastReadTime = java.time.LocalDateTime.parse(lastReadTimeStr);
        }

        Result result = feedService.getUnreadFeedCountFromDB(userId, lastReadTime);
        resp.getWriter().write(JSON.toJSONString(result));
    }


    /**
     * 处理手动推送Feed（测试用）
     */
    private void handlePushFeed(Long userId, Long videoId, HttpServletResponse resp) throws IOException {
        Result result = feedService.pushFeedToFollowers(userId, videoId, java.time.LocalDateTime.now());
        resp.getWriter().write(JSON.toJSONString(result));
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
}