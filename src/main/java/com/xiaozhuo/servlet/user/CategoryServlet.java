
package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CategoryService;
import com.xiaozhuo.entity.VideoCategory;
import com.xiaozhuo.util.JDBCUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/category/*")
public class CategoryServlet extends HttpServlet {

    private CategoryService categoryService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.categoryService = context.getBean(CategoryService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllCategories(resp);
            } else if (pathInfo.matches("/\\d+")) {
                Long categoryId = Long.parseLong(pathInfo.substring(1));
                handleGetCategoryById(categoryId, resp);
            } else if (pathInfo.matches("/sub/\\d+")) {
                Long parentId = Long.parseLong(pathInfo.substring(5));
                handleGetSubCategories(parentId, resp);
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

            if (!isAdmin(userId)) {
                throw new BusinessException(403, "无权操作，仅管理员可创建分类");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                handleCreateCategory(req, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "未登录，请先登录");
            }

            if (!isAdmin(userId)) {
                throw new BusinessException(403, "无权操作，仅管理员可更新分类");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new BusinessException(400, "请求路径错误");
            }

            Long categoryId = Long.parseLong(pathInfo.substring(1));
            handleUpdateCategory(req, resp, categoryId);
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

            if (!isAdmin(userId)) {
                throw new BusinessException(403, "无权操作，仅管理员可删除分类");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new BusinessException(400, "请求路径错误");
            }

            Long categoryId = Long.parseLong(pathInfo.substring(1));
            handleDeleteCategory(categoryId, resp);
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    private void handleCreateCategory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            body.append(line);
        }

        if (body.length() == 0) {
            resp.getWriter().write(JSON.toJSONString(Result.fail(400, "请求体不能为空")));
            return;
        }

        VideoCategory category = JSON.parseObject(body.toString(), VideoCategory.class);
        Result result = categoryService.createCategory(category);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleUpdateCategory(HttpServletRequest req, HttpServletResponse resp, Long categoryId) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            body.append(line);
        }

        if (body.length() == 0) {
            resp.getWriter().write(JSON.toJSONString(Result.fail(400, "请求体不能为空")));
            return;
        }

        VideoCategory category = JSON.parseObject(body.toString(), VideoCategory.class);
        category.setId(categoryId);
        Result result = categoryService.updateCategory(category);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleDeleteCategory(Long categoryId, HttpServletResponse resp) throws IOException {
        Result result = categoryService.deleteCategory(categoryId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetAllCategories(HttpServletResponse resp) throws IOException {
        Result result = categoryService.getAllCategories();
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetCategoryById(Long categoryId, HttpServletResponse resp) throws IOException {
        Result result = categoryService.getCategoryById(categoryId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetSubCategories(Long parentId, HttpServletResponse resp) throws IOException {
        Result result = categoryService.getSubCategories(parentId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private boolean isAdmin(Long userId) {
        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            String sql = "SELECT role FROM sys_user WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Integer role = rs.getInt("role");
                return role != null && role == 1;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }
}
