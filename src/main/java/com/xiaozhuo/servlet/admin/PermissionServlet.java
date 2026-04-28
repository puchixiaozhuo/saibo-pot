package com.xiaozhuo.servlet.admin;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.PermissionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * 权限管理 Servlet（管理员专用）
 * 使用 IoC 容器 + 全局异常处理
 */
@WebServlet("/api/admin/permission/*")
public class PermissionServlet extends HttpServlet {

    private PermissionService permissionService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.permissionService = context.getBean(PermissionService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    /**
     * GET 请求处理
     * - /api/admin/permission/my-permissions - 获取当前用户权限列表
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
            if ("/my-permissions".equals(pathInfo)) {
                handleGetMyPermissions(userId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    /**
     * 获取当前用户权限列表
     */
    private void handleGetMyPermissions(Long userId, HttpServletResponse resp) throws IOException {
        Result<Set<String>> result = permissionService.getUserPermissions(userId);
        resp.getWriter().write(JSON.toJSONString(result));
    }
}
