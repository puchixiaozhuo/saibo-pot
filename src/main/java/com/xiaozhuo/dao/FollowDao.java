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
}