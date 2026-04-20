package com.xiaozhuo.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Token 工具类：生成、解析、刷新
 * 考核要求：登录生成 Token，实现 Token 刷新机制
 */
public class TokenUtil {
    // Token 过期时间：2 小时（单位：毫秒）
    public static final long TOKEN_EXPIRE_TIME = 2 * 60 * 60 * 1000L;
    // Token 分隔符
    private static final String SEPARATOR = "_";

    /**
     * 生成 Token
     * @param userId 用户 ID（主键，唯一）
     * @return Base64 编码的 Token 字符串（格式：userId_UUID_时间戳）
     * @throws RuntimeException 生成失败时抛出
     */
    public static String generateToken(Long userId) {
        try {
            String tokenContent = userId + SEPARATOR + UUID.randomUUID().toString().replace("-", "") + SEPARATOR + System.currentTimeMillis();
            byte[] bytes = tokenContent.getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("生成 Token 失败", e);
        }
    }

    /**
     * 解析 Token，提取用户 ID
     * @param token 登录生成的 Token
     * @return 用户 ID
     * @throws RuntimeException Token 为空或解析失败时抛出
     */
    public static Long parseUserId(String token) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(token);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decoded.split(SEPARATOR);
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            throw new RuntimeException("Token 解析失败", e);
        }
    }

    /**
     * 刷新 Token：为用户生成新的 Token
     * @param userId 用户 ID
     * @return 新的 Token 字符串
     */
    public static String refreshToken(Long userId) {
        return generateToken(userId);
    }

    /**
     * 测试主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        Long userId = 1L;
        String token = TokenUtil.generateToken(userId);
        System.out.println("生成的 Token：" + token);
        String newToken = TokenUtil.refreshToken(userId);
        System.out.println("刷新后的 Token：" + newToken);

        Long parseId = TokenUtil.parseUserId(token);
        System.out.println("解析的用户 ID：" + parseId);
    }
}

