package com.xiaozhuo.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 密码加盐加密工具类（MD5）
 * 考核要求：密码不能明文存储，需哈希加盐加密
 */
public class MD5Util {
    private static final String CHARSET = "UTF-8";
    private static final String MD5_ALGORITHM = "MD5";

    /**
     * 生成 32 位随机盐值（UUID）
     * @return 32 位小写 UUID 字符串（无横杠）
     */
    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    /**
     * 明文密码 + 盐值 加密
     * @param rawPwd 明文密码
     * @param salt 盐值（由 generateSalt 方法生成）
     * @return 32 位 MD5 密文字符串
     * @throws RuntimeException MD5 算法不存在或加密失败时抛出
     */
    public static String encrypt(String rawPwd, String salt) {
        try {
            String content = rawPwd + salt;
            MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
            byte[] bytes = md.digest(content.getBytes(CHARSET));
            return byteToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 加密算法不存在", e);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串（内部工具方法）
     * @param bytes MD5 加密后的字节数组
     * @return 32 位十六进制字符串
     */
    private static String byteToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 测试主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        String rawPwd = "123456";
        String salt = MD5Util.generateSalt();
        String cipherPwd = MD5Util.encrypt(rawPwd, salt);
        System.out.println("明文密码：" + rawPwd);
        System.out.println("盐值：" + salt);
        System.out.println("加密后密文：" + cipherPwd);
    }
}