package com.xiaozhuo.servlet.user;

import com.xiaozhuo.bean.vo.UserVO;
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

/**
 * 获取用户个人信息 Servlet
 */
@WebServlet("/api/user/info")
public class UserInfoServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        System.out.println("========== UserInfoServlet 被调用 ==========");
        System.out.println("[Servlet] 请求IP: " + req.getRemoteAddr());

        resp.setContentType("application/json;charset=UTF-8");

        try {
            String userIdParam = req.getParameter("userId");
            System.out.println("[Servlet] 接收到的 userId 参数: " + userIdParam);

            if (userIdParam == null || userIdParam.trim().isEmpty()) {
                System.out.println("[Servlet] 错误：userId 参数为空");
                Result<UserVO> result = Result.fail(400, "用户ID不能为空");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            Long userId;
            try {
                userId = Long.parseLong(userIdParam.trim());
            } catch (NumberFormatException e) {
                System.out.println("[Servlet] 错误：userId 格式不正确：" + userIdParam);
                Result<UserVO> result = Result.fail(400, "用户ID格式错误");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            if (userId <= 0) {
                System.out.println("[Servlet] 错误：userId 必须为正整数");
                Result<UserVO> result = Result.fail(400, "用户ID必须为正整数");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            Result<UserVO> result = userService.getUserInfo(userId);
            System.out.println("[Servlet] Service 返回结果：" + JsonUtil.toJson(result));

            resp.getWriter().write(JsonUtil.toJson(result));
            System.out.println("========== UserInfoServlet 执行完毕 ==========");

        } catch (Exception e) {
            System.out.println("[Servlet] 发生异常：" + e.getMessage());
            e.printStackTrace();
            Result<UserVO> result = Result.error();
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Result<UserVO> result = Result.fail(405, "不支持 POST 请求，请使用 GET");
        resp.getWriter().write(JsonUtil.toJson(result));
    }
}

