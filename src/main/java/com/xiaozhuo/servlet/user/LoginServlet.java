package com.xiaozhuo.servlet.user;

import com.xiaozhuo.bean.dto.LoginDTO;
import com.xiaozhuo.bean.vo.LoginVO;
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
 * 用户登录 Servlet
 */
@WebServlet("/api/user/login")
public class LoginServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        System.out.println("========== LoginServlet 被调用 ==========");
        System.out.println("[Servlet] 请求IP: " + req.getRemoteAddr());

        // 设置响应编码
        resp.setContentType("application/json;charset=UTF-8");


        try {
            // 1. 读取请求体 JSON 数据
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString().trim();
            System.out.println("[Servlet] 接收到的原始 JSON: " + json);

            // 检查 JSON 是否为空
            if (json == null || json.isEmpty() || json.equals("{}")) {
                System.out.println("[Servlet] 错误：JSON 为空");
                Result<LoginVO> result = Result.fail(400, "请求体不能为空");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            // 验证 JSON 格式
            if (!json.startsWith("{") || !json.endsWith("}")) {
                System.out.println("[Servlet] 错误：JSON 格式不正确：" + json);
                Result<LoginVO> result = Result.fail(400, "JSON 格式错误");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            // 2. 转换为 DTO 对象
            LoginDTO dto;
            try {
                dto = JsonUtil.fromJson(json, LoginDTO.class);
            } catch (Exception e) {
                System.out.println("[Servlet] JSON 解析失败：" + e.getMessage());
                e.printStackTrace();
                Result<LoginVO> result = Result.fail(400, "JSON 格式错误：" + e.getMessage());
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            if (dto == null) {
                System.out.println("[Servlet] 错误：DTO 为 null");
                Result<LoginVO> result = Result.fail(400, "无法解析登录数据");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                System.out.println("[Servlet] 错误：密码为空");
                Result<LoginVO> result = Result.fail(400, "密码不能为空");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            System.out.println("[Servlet] 解析后的 DTO: username=" + dto.getUsername() +
                    ", password=" + (dto.getPassword() != null ? "***" : "null"));

            // 3. 调用 Service 层处理
            Result<LoginVO> result = userService.login(dto);
            System.out.println("[Servlet] Service 返回结果：" + JsonUtil.toJson(result));

            // 4. 返回结果
            resp.getWriter().write(JsonUtil.toJson(result));
            System.out.println("========== LoginServlet 执行完毕 ==========");

        } catch (Exception e) {
            System.out.println("[Servlet] 发生异常：" + e.getMessage());
            e.printStackTrace();
            Result<LoginVO> result = Result.error();
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // GET 请求返回错误提示
        resp.setContentType("application/json;charset=UTF-8");
        Result<LoginVO> result = Result.fail(405, "不支持 GET 请求，请使用 POST");
        resp.getWriter().write(JsonUtil.toJson(result));
    }
}

