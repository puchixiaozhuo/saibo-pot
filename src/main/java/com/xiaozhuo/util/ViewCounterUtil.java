package com.xiaozhuo.util;

import java.time.LocalDate;

/**
 * 播放量防刷工具类
 */
public class ViewCounterUtil {

    /**
     * 记录一次有效播放（带防刷逻辑）
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否计入播放量
     */
    public static boolean recordValidView(Long userId, Long videoId) {
        if (userId == null || videoId == null) return false;

        // Key: view:daily:{date}:{userId}:{videoId}
        String key = String.format("view:daily:%s:%d:%d", LocalDate.now(), userId, videoId);

        // SETNX: 如果 key 不存在则设为 1，返回 true；如果已存在，返回 false
        Boolean isNew = RedisUtil.setnx(key, "1");

        if (Boolean.TRUE.equals(isNew)) {
            // 设置过期时间为 2 天，节省空间
            RedisUtil.expire(key, 172800);
            return true;
        }
        return false;
    }
}