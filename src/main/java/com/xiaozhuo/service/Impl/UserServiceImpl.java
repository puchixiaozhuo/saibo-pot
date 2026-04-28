package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.bean.dto.LoginDTO;
import com.xiaozhuo.bean.dto.UserDTO;
import com.xiaozhuo.bean.vo.LoginVO;
import com.xiaozhuo.bean.vo.UserVO;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.UserService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.MD5Util;
import com.xiaozhuo.util.TokenUtil;
import com.xiaozhuo.util.TransactionManager;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户业务实现类（优化版）
 * 使用声明式事务 + IoC 容器管理
 */
@Bean
public class UserServiceImpl implements UserService {

    private UserDao userDao = new UserDaoImpl();

    /**
     * 用户注册（声明式事务）
     */
    @Transactional
    @Override
    public Result<Map<String, Object>> register(UserDTO dto) {
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        String username = dto.getUsername().trim();
        String password = dto.getPassword();
        String nickname = dto.getNickname() != null ? dto.getNickname().trim() : "用户_" + System.currentTimeMillis() % 10000;
        Integer role = dto.getType() != null ? dto.getType() : 0;

        if (role != 0 && role != 1) {
            throw new BusinessException(400, "角色类型无效，只能为 0（普通用户）或 1（管理员）");
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException(400, "用户名长度应为 3-20 位");
        }

        if (password.length() < 6 || password.length() > 20) {
            throw new BusinessException(400, "密码长度应为 6-20 位");
        }

        try {
            Connection conn = TransactionManager.getConnection();

            User existUser = userDao.findByUsername(conn, username);
            if (existUser != null) {
                throw new BusinessException(400, "用户名已存在");
            }

            String salt = MD5Util.generateSalt();
            String encryptedPassword = MD5Util.encrypt(password, salt);

            User user = new User();
            user.setUsername(username);
            user.setPassword(encryptedPassword);
            user.setSalt(salt);
            user.setNickname(nickname);
            user.setRole(role);
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            int rows = userDao.insert(conn, user);
            if (rows > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("userId", user.getId());
                data.put("username", user.getUsername());
                data.put("nickname", user.getNickname());
                data.put("role", user.getRole());
                return Result.success("注册成功", data);
            } else {
                throw new BusinessException(500, "注册失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "注册失败：" + e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @Override
    public Result<LoginVO> login(LoginDTO dto) {
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        String username = dto.getUsername().trim();
        String password = dto.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            User user = userDao.findByUsername(conn, username);
            if (user == null) {
                throw new BusinessException(401, "用户名或密码错误");
            }

            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(403, "账号已被禁用，请联系管理员");
            }

            String encryptedPassword = MD5Util.encrypt(password, user.getSalt());
            if (!encryptedPassword.equals(user.getPassword())) {
                throw new BusinessException(401, "用户名或密码错误");
            }

            LocalDateTime now = LocalDateTime.now();
            userDao.updateLoginTime(conn, user.getId(), now);

            LoginVO loginVO = buildLoginVO(user);

            return Result.success("登录成功", loginVO);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "登录失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }


    /**
     * 获取用户个人信息
     */
    @Override
    public Result<UserVO> getUserInfo(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            User user = userDao.findById(conn, userId);
            if (user == null) {
                throw new BusinessException(404, "用户不存在");
            }

            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(403, "账号已被禁用");
            }

            UserVO userVO = buildUserVO(user);
            return Result.success("查询成功", userVO);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "查询失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }


    /**
     * 刷新 Token
     */
    @Override
    public Result<LoginVO> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException(400, "refreshToken 不能为空");
        }

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            Long userId = TokenUtil.parseUserId(refreshToken);

            User user = userDao.findById(conn, userId);
            if (user == null) {
                throw new BusinessException(401, "用户不存在");
            }

            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(403, "账号已被禁用");
            }

            LoginVO loginVO = buildLoginVO(user);

            return Result.success("Token 刷新成功", loginVO);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "refreshToken 无效或已过期");
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 构建 LoginVO 对象
     */
    private LoginVO buildLoginVO(User user) {
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());

        UserVO userVO = buildUserVO(user);
        loginVO.setUserInfo(userVO);

        String accessToken = TokenUtil.generateToken(user.getId());
        String refreshToken = TokenUtil.refreshToken(user.getId());

        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setExpiresIn(TokenUtil.TOKEN_EXPIRE_TIME);

        return loginVO;
    }

    /**
     * 构建 UserVO 对象
     */
    private UserVO buildUserVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());
        userVO.setCreateTime(user.getCreateTime());
        return userVO;
    }
}
