package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CouponService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/coupon/batch/*")
public class CouponBatchServlet extends HttpServlet {

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

        String pathInfo = req.getPathInfo();

        try {
            if ("/release".equals(pathInfo)) {
                handleRelease(resp);
            } else if (pathInfo != null && pathInfo.startsWith("/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length >= 2) {
                    Long activityId = Long.parseLong(parts[1]);
                    handleGetBatches(activityId, resp);
                } else {
                    resp.getWriter().write(JSON.toJSONString(Result.fail(400, "参数错误")));
                }
            } else {
                resp.getWriter().write(JSON.toJSONString(Result.fail(400, "路径错误")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(JSON.toJSONString(Result.error()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    private void handleRelease(HttpServletResponse resp) throws IOException {
        Result<Integer> result = couponService.releasePendingBatches();
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetBatches(Long activityId, HttpServletResponse resp) throws IOException {
        Result<List<Map<String, Object>>> result = couponService.getActivityBatches(activityId);
        resp.getWriter().write(JSON.toJSONString(result));
    }
}

