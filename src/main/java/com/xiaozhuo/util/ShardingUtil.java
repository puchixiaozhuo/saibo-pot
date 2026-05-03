package com.xiaozhuo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 分表路由工具类
 * 提供垂直分表和水平分表的路由逻辑
 */
public class ShardingUtil {


    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");


    /**
     * 获取用户相关表的哈希后缀（按 user_id % 16 分表）
     * @param userId 用户ID
     * @return 表后缀索引（0-15）
     */
    public static int getUserTableSuffix(Long userId) {
        if (userId == null) {
            return 0;
        }
        return (int) (Math.abs(userId) % 16);
    }

    /**
     * 构建用户相关分表表名
     * @param baseTableName 基础表名（如：user_watch_history）
     * @param userId 用户ID
     * @return 完整表名（如：user_watch_history_5）
     */
    public static String getUserTableName(String baseTableName, Long userId) {
        int suffix = getUserTableSuffix(userId);
        return baseTableName + "_" + suffix;
    }

    /**
     * 获取评论表后缀（按月分表）
     * @param createTime 评论创建时间
     * @return 表后缀，如：202605
     */
    public static String getCommentTableSuffix(LocalDateTime createTime) {
        if (createTime == null) {
            return "";
        }
        return createTime.format(MONTH_FORMATTER);
    }

    /**
     * 构建评论分表表名
     * @param baseTableName 基础表名（如：video_comment）
     * @param createTime 创建时间
     * @return 完整表名（如：video_comment_202605）
     */
    public static String getCommentTableName(String baseTableName, LocalDateTime createTime) {
        String suffix = getCommentTableSuffix(createTime);
        if (suffix.isEmpty()) {
            return baseTableName;
        }
        return baseTableName + "_" + suffix;
    }

    /**
     * 获取最近 N 个月的分表名称数组
     */
    public static String[] getRecentMonthTables(String baseTableName, int months) {
        String[] tables = new String[months];
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < months; i++) {
            LocalDateTime date = now.minusMonths(i);
            String suffix = date.format(MONTH_FORMATTER);
            tables[i] = baseTableName + "_" + suffix;
        }
        return tables;
    }
}
