// 优化后的RefreshTokenServlet.java
package com.xiaozhuo.servlet.user;

import com.xiaozhuo.bean.vo.LoginVO;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.factory.ServiceFactory;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.UserService;
import com.xiaozhuo.service.Impl.UserServiceImpl;
import com.xiaozhuo.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/user/refresh")
public class RefreshTokenServlet extends HttpServlet {

    // 改为使用工厂
    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = ServiceFactory.getUserService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString().trim();
            Map<String, String> requestBody = JsonUtil.fromJson(json, Map.class);

            String refreshToken = requestBody.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new BusinessException(400, "refreshToken不能为空");
            }

            Result<LoginVO> result = userService.refreshToken(refreshToken);
            resp.getWriter().write(JsonUtil.toJson(result));

        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }
}