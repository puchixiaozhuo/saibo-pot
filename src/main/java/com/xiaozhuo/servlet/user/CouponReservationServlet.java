package com.xiaozhuo.servlet.user;

import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CouponService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 优惠券预约抽签 Servlet
 */
@WebServlet("/api/coupon/reservation/*")
public class CouponReservationServlet extends HttpServlet {

    private static final Logger logger = LogUtil.getLogger(CouponReservationServlet.class);
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

            if ("/reserve".equals(pathInfo)) {
                handleReserve(req, resp);
            } else if ("/lottery".equals(pathInfo)) {
                handleLottery(req, resp);
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

            if (pathInfo != null && pathInfo.matches("/status/\\d+")) {
                handleGetStatus(req, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 处理预约
     * POST /api/coupon/reservation/reserve?activityId=X
     */
    private void handleReserve(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        try {
            Long activityId = Long.parseLong(req.getParameter("activityId"));

            Result<Boolean> result = couponService.reserveCoupon(userId, activityId);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            throw new BusinessException(400, "参数格式错误");
        }
    }

    /**
     * 执行抽签（仅管理员）
     * POST /api/coupon/reservation/lottery?activityId=X
     */
    private void handleLottery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 🔥 权限校验：检查是否为管理员
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        // 检查用户角色
        if (!isAdmin(userId)) {
            LogUtil.logBusiness(logger, "UNAUTHORIZED_LOTTERY",
                "User " + userId + " attempted to execute lottery without admin permission");
            throw new BusinessException(403, "无权执行抽签，仅管理员可操作");
        }

        try {
            Long activityId = Long.parseLong(req.getParameter("activityId"));

            Result<Integer> result = couponService.executeLottery(activityId);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            throw new BusinessException(400, "参数格式错误");
        }
    }

    /**
     * 查询预约状态
     * GET /api/coupon/reservation/status/{activityId}
     */
    private void handleGetStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        try {
            String pathInfo = req.getPathInfo();
            Long activityId = Long.parseLong(pathInfo.substring("/status/".length()));

            Result<?> result = couponService.getMyReservationStatus(userId, activityId);
            resp.getWriter().write(com.alibaba.fastjson.JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            throw new BusinessException(400, "参数格式错误");
        }
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean isAdmin(Long userId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            System.out.println("🔍 Debug - Checking admin role for userId: " + userId);

            // 查询用户角色（role 是 TINYINT 类型：0-普通用户，1-管理员）
            String sql = "SELECT role FROM sys_user WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int role = rs.getInt("role");
                    System.out.println("🔍 Debug - User role from DB: " + role);

                    boolean isAdmin = (role == 1);
                    System.out.println("🔍 Debug - Is admin check result: " + isAdmin);

                    return isAdmin;
                } else {
                    System.out.println("❌ Debug - No user found with id: " + userId);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Debug - SQL Exception: " + e.getMessage());
            LogUtil.logError(logger, "查询用户角色失败: userId=" + userId, e);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
        System.out.println("❌ Debug - Returning false (default)");
        return false;
    }
}
