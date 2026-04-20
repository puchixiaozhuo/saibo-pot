package com.xiaozhuo.servlet.user;

import com.xiaozhuo.bean.dto.UserDTO;
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

/**
 * 用户注册 Servlet
 */
@WebServlet("/api/user/register")
public class RegisterServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        System.out.println("========== RegisterServlet 被调用 ==========");

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

            // 检查 JSON 是否为空或格式错误
            if (json == null || json.isEmpty() || json.equals("{}")) {
                System.out.println("[Servlet] 错误：JSON 为空");
                Result<Map<String, Object>> result = Result.fail(400, "请求体不能为空");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            // 验证 JSON 格式是否正确
            if (!json.startsWith("{") || !json.endsWith("}")) {
                System.out.println("[Servlet] 错误：JSON 格式不正确：" + json);
                Result<Map<String, Object>> result = Result.fail(400, "JSON 格式错误，请以 { 开头，以 } 结尾");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            // 2. 转换为 DTO 对象
            UserDTO dto;
            try {
                dto = JsonUtil.fromJson(json, UserDTO.class);
            } catch (Exception e) {
                System.out.println("[Servlet] JSON 解析失败：" + e.getMessage());
                e.printStackTrace();
                Result<Map<String, Object>> result = Result.fail(400, "JSON 格式错误：" + e.getMessage());
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            // 检查 DTO 是否为 null
            if (dto == null) {
                System.out.println("[Servlet] 错误：DTO 为 null");
                Result<Map<String, Object>> result = Result.fail(400, "无法解析用户数据");
                resp.getWriter().write(JsonUtil.toJson(result));
                return;
            }

            System.out.println("[Servlet] 解析后的 DTO: username=" + dto.getUsername() +
                    ", password=" + (dto.getPassword() != null ? "***" : "null") +
                    ", nickname=" + dto.getNickname());

            // 3. 调用 Service 层处理
            Result<Map<String, Object>> result = userService.register(dto);
            System.out.println("[Servlet] Service 返回结果：" + JsonUtil.toJson(result));
            // 4. 返回结果
            resp.getWriter().write(JsonUtil.toJson(result));
            System.out.println("========== RegisterServlet 执行完毕 ==========");


        } catch (Exception e) {
            System.out.println("[Servlet] 发生异常：" + e.getMessage());
            e.printStackTrace();
            Result<Map<String, Object>> result = Result.error();
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // GET 请求返回错误提示
        resp.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(405, "不支持 GET 请求，请使用 POST");
        resp.getWriter().write(JsonUtil.toJson(result));
    }
}

