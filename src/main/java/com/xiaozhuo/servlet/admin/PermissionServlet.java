package com.xiaozhuo.servlet.admin;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.service.PermissionService;
import com.xiaozhuo.service.Impl.PermissionServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/admin/permission/*")
public class PermissionServlet extends HttpServlet {

    private PermissionService permissionService = new PermissionServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录")));
            return;
        }

        String pathInfo = req.getPathInfo();
        if ("/my-permissions".equals(pathInfo)) {
            Map<String, Object> result = permissionService.getUserPermissions(userId);
            out.print(JSON.toJSONString(result));
        } else {
            out.print(JSON.toJSONString(errorResult(404, "接口不存在")));
        }
    }

    private Map<String, Object> errorResult(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        return result;
    }
}