package com.xiaozhuo.dao;

import com.xiaozhuo.entity.UserFollow;
import java.sql.Connection;
import java.util.List;

public interface FollowDao {
    int insert(Connection conn, UserFollow userFollow);

    int deleteByUserIdAndFollowId(Connection conn, Long userId, Long followId);

    UserFollow selectByUserIdAndFollowId(Connection conn, Long userId, Long followId);

    List<UserFollow> selectFollowingList(Connection conn, Long userId, int pageNum, int pageSize);

    List<UserFollow> selectFollowerList(Connection conn, Long followId, int pageNum, int pageSize);

    int countFollowing(Connection conn, Long userId);

    int countFollower(Connection conn, Long followId);

    boolean isFollowing(Connection conn, Long userId, Long followId);

    /**
     * 获取用户关注的所有用户ID列表
     * @param conn 数据库连接
     * @param userId 用户ID
     * @return 关注的用户ID列表
     */
    List<Long> getFollowingUserIds(Connection conn, Long userId);

    /**
     * 获取用户的所有粉丝ID列表
     * @param conn 数据库连接
     * @param userId 用户ID（博主）
     * @return 粉丝ID列表
     */
    List<Long> getFollowerUserIds(Connection conn, Long userId);
}