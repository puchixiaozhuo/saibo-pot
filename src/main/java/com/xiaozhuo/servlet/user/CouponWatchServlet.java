package com.xiaozhuo.servlet.user;

import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CouponService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 观看时长解锁优惠券 Servlet
 */
@WebServlet("/api/coupon/watch/*")
public class CouponWatchServlet extends HttpServlet {

    private CouponService couponService;

    @Override
    public void init() throws ServletException {
        super.init();
        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.couponService = context.getBean(CouponService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();

            if ("/report".equals(pathInfo)) {
                handleReportProgress(req, resp);
            } else if ("/claim".equals(pathInfo)) {
                handleClaimCoupon(req, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();

            if ("/status".equals(pathInfo)) {
                handleCheckStatus(req, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 上报观看进度
     * POST /api/coupon/watch/report?videoId=X&activityId=X&watchedSeconds=X
     */
    private void handleReportProgress(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        try {
            Long videoId = Long.parseLong(req.getParameter("videoId"));
            Long activityId = Long.parseLong(req.getParameter("activityId"));
            Integer watchedSeconds = Integer.parseInt(req.getParameter("watchedSeconds"));

            Result<Boolean> result = couponService.reportWatchProgress(userId, videoId, activityId, watchedSeconds);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            throw new BusinessException(400, "参数格式错误");
        }
    }

    /**
     * 检查解锁状态
     * GET /api/coupon/watch/status?videoId=X&activityId=X
     */
    private void handleCheckStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        try {
            Long videoId = Long.parseLong(req.getParameter("videoId"));
            Long activityId = Long.parseLong(req.getParameter("activityId"));

            Result<?> result = couponService.checkUnlockStatus(userId, videoId, activityId);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            throw new BusinessException(400, "参数格式错误");
        }
    }

    /**
     * 领取优惠券
     * POST /api/coupon/watch/claim?activityId=X
     */
    private void handleClaimCoupon(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        try {
            Long activityId = Long.parseLong(req.getParameter("activityId"));

            Result<?> result = couponService.claimWatchUnlockCoupon(userId, activityId);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            throw new BusinessException(400, "参数格式错误");
        }
    }
}
