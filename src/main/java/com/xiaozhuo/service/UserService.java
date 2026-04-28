package com.xiaozhuo.service;

import com.xiaozhuo.bean.dto.LoginDTO;
import com.xiaozhuo.bean.dto.UserDTO;
import com.xiaozhuo.bean.vo.LoginVO;
import com.xiaozhuo.bean.vo.UserVO;
import com.xiaozhuo.result.Result;

import java.util.Map;

/**
 * 用户业务接口
 */
public interface UserService {
    /**
     * 用户注册
     * @param userDTO 注册请求参数（用户名、密码）
     * @return 注册结果（成功/失败）
     */
    Result<Map<String, Object>> register(UserDTO userDTO);

    /**
     * 用户登录
     * @param dto 登录请求参数（用户名、密码）
     * @return 登录结果（成功/失败）
     */
    Result<LoginVO> login(LoginDTO dto);

    /**
     * 获取用户个人信息
     * @param userId 用户ID
     * @return 用户信息
     */
    Result<UserVO> getUserInfo(Long userId);

    /**
     * 刷新 Token
     * @param refreshToken 刷新令牌
     * @return 新的 Token 信息
     */
    Result<LoginVO> refreshToken(String refreshToken);
}
