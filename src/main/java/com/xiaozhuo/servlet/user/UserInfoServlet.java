// 优化后的UserInfoServlet.java
package com.xiaozhuo.servlet.user;

import com.xiaozhuo.bean.vo.UserVO;
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

@WebServlet("/api/user/info")
public class UserInfoServlet extends HttpServlet {

    // 改为使用工厂
    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = ServiceFactory.getUserService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            // 从请求属性中获取userId（由AuthFilter设置）
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "用户未登录");
            }

            Result<UserVO> result = userService.getUserInfo(userId);
            resp.getWriter().write(JsonUtil.toJson(result));

        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }
}