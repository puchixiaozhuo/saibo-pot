package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.WatchHistoryService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/watch-history/*")
public class WatchHistoryServlet extends HttpServlet {

    private WatchHistoryService watchHistoryService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.watchHistoryService = context.getBean(WatchHistoryService.class);
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
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetWatchHistory(req, resp, userId);
            } else if (pathInfo.matches("/video/\\d+")) {
                Long videoId = Long.parseLong(pathInfo.substring(7));
                handleGetWatchRecord(userId, videoId, resp);
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
            if (pathInfo == null || pathInfo.equals("/")) {
                handleReportProgress(req, resp, userId);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

            if (pathInfo.matches("/\\d+")) {
                Long historyId = Long.parseLong(pathInfo.substring(1));
                handleDeleteWatchRecord(userId, historyId, resp);
            } else if (pathInfo.equals("/clear")) {
                handleClearWatchHistory(userId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    private void handleReportProgress(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            body.append(line);
        }

        if (body.length() == 0) {
            resp.getWriter().write(JSON.toJSONString(Result.fail(400, "请求体不能为空")));
            return;
        }

        Map<String, Object> data = JSON.parseObject(body.toString(), Map.class);
        Long videoId = Long.valueOf(data.get("videoId").toString());
        Integer progress = data.get("progress") != null ? Integer.valueOf(data.get("progress").toString()) : 0;
        Integer duration = data.get("duration") != null ? Integer.valueOf(data.get("duration").toString()) : 0;

        Result result = watchHistoryService.reportWatchProgress(userId, videoId, progress, duration);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetWatchHistory(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result result = watchHistoryService.getWatchHistory(userId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetWatchRecord(Long userId, Long videoId, HttpServletResponse resp) throws IOException {
        Result result = watchHistoryService.getWatchRecord(userId, videoId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleDeleteWatchRecord(Long userId, Long historyId, HttpServletResponse resp) throws IOException {
        Result result = watchHistoryService.deleteWatchRecord(userId, historyId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleClearWatchHistory(Long userId, HttpServletResponse resp) throws IOException {
        Result result = watchHistoryService.clearWatchHistory(userId);
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