package com.xiaozhuo.service.Impl;

import com.xiaozhuo.bean.dto.LoginDTO;
import com.xiaozhuo.bean.dto.UserDTO;
import com.xiaozhuo.bean.vo.LoginVO;
import com.xiaozhuo.bean.vo.UserVO;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.UserService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.MD5Util;
import com.xiaozhuo.util.TokenUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户业务实现类，用于处理用户相关的逻辑
 */
public class UserServiceImpl implements UserService {
    private UserDao userDao = new UserDaoImpl();

    /**
     * 用户注册
     *
     * @param dto 用户注册信息
     * @return 注册结果
     */
    public Result<Map<String, Object>> register(UserDTO dto) {
        // 1. 参数校验
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            return Result.paramError();
        }

        String username = dto.getUsername().trim();
        String password = dto.getPassword();
        String nickname = dto.getNickname() != null ? dto.getNickname().trim() : "用户_" + System.currentTimeMillis() % 10000;
        Integer role = dto.getType() != null ? dto.getType() : 0;

        if (role != 0 && role != 1) {
            return Result.fail(400, "角色类型无效，只能为 0（普通用户）或 1（管理员）");
        }

        // 校验用户名长度（3-20 位）
        if (username.length() < 3 || username.length() > 20) {
            return Result.fail(400, "用户名长度应为 3-20 位");
        }

        // 校验密码长度（6-20 位）
        if (password.length() < 6 || password.length() > 20) {
            return Result.fail(400, "密码长度应为 6-20 位");
        }

        Connection conn = null;
        try {
            // 2. 获取数据库连接
            conn = JDBCUtil.getConnection();
            conn.setAutoCommit(false); // 开启事务

            // 3. 检查用户名是否已存在
            User existUser = userDao.findByUsername(conn, username);
            if (existUser != null) {
                conn.rollback();
                return Result.fail(400, "用户名已存在");
            }

            // 4. 生成盐值并加密密码
            String salt = MD5Util.generateSalt();
            String encryptedPassword = MD5Util.encrypt(password, salt);

            // 5. 创建用户对象
            User user = new User();
            user.setUsername(username);
            user.setPassword(encryptedPassword);
            user.setSalt(salt);
            user.setNickname(nickname);
            user.setRole(role);
            user.setStatus(1); // 默认正常状态
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            // 6. 插入数据库
            int rows = userDao.insert(conn, user);
            if (rows > 0) {
                conn.commit();
                Map<String, Object> data = new HashMap<>();
                data.put("userId", user.getId());
                data.put("username", user.getUsername());
                data.put("nickname", user.getNickname());
                data.put("role", user.getRole());
                return Result.success("注册成功", data);
            } else {
                conn.rollback();
                return Result.error();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return Result.error();
        } finally {
            // 7. 释放资源
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return 登录结果
     */
    public Result<LoginVO> login(LoginDTO dto) {
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            return Result.paramError();
        }

        String username = dto.getUsername().trim();
        String password = dto.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            return Result.fail(400, "用户名和密码不能为空");
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            User user = userDao.findByUsername(conn, username);
            if (user == null) {
                return Result.fail(401, "用户名或密码错误");
            }

            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.fail(403, "账号已被禁用，请联系管理员");
            }

            String encryptedPassword = MD5Util.encrypt(password, user.getSalt());
            if (!encryptedPassword.equals(user.getPassword())) {
                return Result.fail(401, "用户名或密码错误");
            }

            LocalDateTime now = LocalDateTime.now();
            userDao.updateLoginTime(conn, user.getId(), now);

            LoginVO loginVO = new LoginVO();
            loginVO.setUserId(user.getId());
            loginVO.setUsername(user.getUsername());
            loginVO.setNickname(user.getNickname());

            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setNickname(user.getNickname());
            userVO.setAvatar(user.getAvatar());
            userVO.setRole(user.getRole());
            userVO.setCreateTime(user.getCreateTime());
            loginVO.setUserInfo(userVO);

            String accessToken = TokenUtil.generateToken(user.getId());
            String refreshToken = TokenUtil.refreshToken(user.getId());

            loginVO.setAccessToken(accessToken);
            loginVO.setRefreshToken(refreshToken);
            loginVO.setExpiresIn(TokenUtil.TOKEN_EXPIRE_TIME);

            return Result.success("登录成功", loginVO);

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 获取用户个人信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public Result<UserVO> getUserInfo(Long userId) {
        if (userId == null || userId <= 0) {
            return Result.fail(400, "用户ID不能为空");
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            User user = userDao.findById(conn, userId);
            if (user == null) {
                return Result.fail(404, "用户不存在");
            }

            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.fail(403, "账号已被禁用");
            }

            UserVO userVO = new UserVO();
            userVO.setId(user.getId());
            userVO.setUsername(user.getUsername());
            userVO.setNickname(user.getNickname());
            userVO.setAvatar(user.getAvatar());
            userVO.setRole(user.getRole());
            userVO.setCreateTime(user.getCreateTime());

            return Result.success("查询成功", userVO);

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


//    /**
//     * 测试主方法 - 直接在控制台测试注册功能
//     * @param args 命令行参数
//     */
//    public static void main(String[] args) {
//        System.out.println("========== 开始测试用户注册功能 ==========");
//
//        // 创建测试数据
//        UserDTO testUser = new UserDTO();
//        testUser.setUsername("testuser2028");
//        testUser.setPassword("12345678");
//        testUser.setNickname("测试用户 2028");
//
//        // 调用注册方法
//        UserServiceImpl userService = new UserServiceImpl();
//        Result<Map<String, Object>> result = userService.register(testUser);
//
//        // 输出结果到控制台
//        System.out.println("\n========== 注册结果 ==========");
//        System.out.println("状态码：" + result.getCode());
//        System.out.println("提示信息：" + result.getMsg());
//
//        if (result.getData() != null) {
//            System.out.println("返回数据：");
//            result.getData().forEach((key, value) ->
//                    System.out.println("  " + key + " = " + value)
//            );
//        }
//
//        // 以 JSON 格式输出完整结果
//        System.out.println("\n========== JSON 格式结果 ==========");
//        System.out.println(JsonUtil.toJson(result));
//        System.out.println("\n========== 测试结束 ==========");
//    }

