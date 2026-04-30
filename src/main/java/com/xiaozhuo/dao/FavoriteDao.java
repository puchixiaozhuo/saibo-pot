package com.xiaozhuo.dao;

import com.xiaozhuo.entity.UserFavorite;

import java.sql.Connection;
import java.util.List;

public interface FavoriteDao {
    /**
     * 添加收藏
     */
    int insert(Connection conn, UserFavorite favorite);

    /**
     * 删除收藏
     */
    int deleteByUserIdAndVideoId(Connection conn, Long userId, Long videoId);

    /**
     * 查询用户是否已收藏
     */
    UserFavorite selectByUserIdAndVideoId(Connection conn, Long userId, Long videoId);

    /**
     * 查询用户的收藏列表（分页）
     */
    List<UserFavorite> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize);

    /**
     * 统计用户收藏数量
     */
    long countByUserId(Connection conn, Long userId);

    /**
     * 统计视频被收藏次数
     */
    long countByVideoId(Connection conn, Long videoId);
}