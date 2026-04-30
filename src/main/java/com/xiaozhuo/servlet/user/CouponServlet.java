package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
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
 * 优惠券Servlet
 */
@WebServlet("/api/coupon/*")
public class CouponServlet extends HttpServlet {

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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.equals("/my")) {
                handleGetMyCoupons(req, resp);
            } else if (pathInfo.matches("/\\d+")) {
                Long activityId = Long.parseLong(pathInfo.substring(1));
                handleGetActivityDetail(activityId, resp);
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
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.matches("/\\d+/grab")) {
                Long activityId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/grab")));
                handleGrabCoupon(userId, activityId, resp);
            } else if (pathInfo.matches("/\\d+/init-stock")) {
                Long activityId = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/init-stock")));
                handleInitStock(activityId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 处理抢购优惠券
     */
    private void handleGrabCoupon(Long userId, Long activityId, HttpServletResponse resp) throws IOException {
        Result result = couponService.grabCoupon(userId, activityId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 处理初始化库存
     */
    private void handleInitStock(Long activityId, HttpServletResponse resp) throws IOException {
        Result result = couponService.initStockToRedis(activityId);
        resp.getWriter().write(JSON.toJSONString(result));
    }


    /**
     * 处理查询我的优惠券
     */
    private void handleGetMyCoupons(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);

        Result result = couponService.getMyCoupons(userId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 处理查询活动详情
     */
    private void handleGetActivityDetail(Long activityId, HttpServletResponse resp) throws IOException {
        Result result = couponService.getActivityDetail(activityId);
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