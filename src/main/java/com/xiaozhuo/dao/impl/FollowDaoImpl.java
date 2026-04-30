package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.FollowDao;
import com.xiaozhuo.entity.UserFollow;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 关注数据访问实现类
 */
public class FollowDaoImpl implements FollowDao {

    private static final Logger logger = LogUtil.getLogger(FollowDaoImpl.class);

    /**
     * 插入关注记录
     */
    @Override
    public int insert(Connection conn, UserFollow userFollow) {
        String sql = "INSERT INTO user_follow (user_id, follow_id, create_time) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userFollow.getUserId());
            ps.setLong(2, userFollow.getFollowId());
            ps.setTimestamp(3, Timestamp.valueOf(userFollow.getCreateTime()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入关注记录失败", e);
            throw new DatabaseException("插入关注记录失败", e);
        }
    }

    /**
     * 删除关注记录
     */
    @Override
    public int deleteByUserIdAndFollowId(Connection conn, Long userId, Long followId) {
        String sql = "DELETE FROM user_follow WHERE user_id = ? AND follow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, followId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "删除关注记录失败: userId=" + userId + ", followId=" + followId, e);
            throw new DatabaseException("删除关注记录失败", e);
        }
    }

    /**
     * 查询关注记录
     */
    @Override
    public UserFollow selectByUserIdAndFollowId(Connection conn, Long userId, Long followId) {
        String sql = "SELECT * FROM user_follow WHERE user_id = ? AND follow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, followId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询关注记录失败: userId=" + userId + ", followId=" + followId, e);
            throw new DatabaseException("查询关注记录失败", e);
        }
        return null;
    }

    /**
     * 查询关注列表
     */
    @Override
    public List<UserFollow> selectFollowingList(Connection conn, Long userId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM user_follow WHERE user_id = ? ORDER BY create_time DESC LIMIT ?, ?";
        return queryFollowList(conn, sql, userId, pageNum, pageSize);
    }

    /**
     * 查询粉丝列表
     */
    @Override
    public List<UserFollow> selectFollowerList(Connection conn, Long followId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM user_follow WHERE follow_id = ? ORDER BY create_time DESC LIMIT ?, ?";
        return queryFollowList(conn, sql, followId, pageNum, pageSize);
    }

    /**
     * 查询关注/粉丝列表（通用方法）
     */
    private List<UserFollow> queryFollowList(Connection conn, String sql, Long id, int pageNum, int pageSize) {
        List<UserFollow> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setInt(2, (pageNum - 1) * pageSize);
            ps.setInt(3, pageSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询关注列表失败: id=" + id, e);
            throw new DatabaseException("查询关注列表失败", e);
        }
        return list;
    }

    /**
     * 统计关注数量
     */
    @Override
    public int countFollowing(Connection conn, Long userId) {
        String sql = "SELECT COUNT(*) FROM user_follow WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计关注数量失败: userId=" + userId, e);
            throw new DatabaseException("统计关注数量失败", e);
        }
        return 0;
    }

    /**
     * 统计粉丝数量
     */
    @Override
    public int countFollower(Connection conn, Long followId) {
        String sql = "SELECT COUNT(*) FROM user_follow WHERE follow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, followId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "统计粉丝数量失败: followId=" + followId, e);
            throw new DatabaseException("统计粉丝数量失败", e);
        }
        return 0;
    }

    /**
     * 判断是否已关注
     */
    @Override
    public boolean isFollowing(Connection conn, Long userId, Long followId) {
        return selectByUserIdAndFollowId(conn, userId, followId) != null;
    }

    /**
     * 获取用户关注的所有用户ID列表
     */
    @Override
    public List<Long> getFollowingUserIds(Connection conn, Long userId) {
        String sql = "SELECT follow_id FROM user_follow WHERE user_id = ?";
        List<Long> followIds = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                followIds.add(rs.getLong("follow_id"));
            }

            LogUtil.logBusiness(logger, "GET_FOLLOWING_USER_IDS",
                    "User " + userId + " follows " + followIds.size() + " users");
        } catch (SQLException e) {
            LogUtil.logError(logger, "获取关注用户ID列表失败: userId=" + userId, e);
            throw new DatabaseException("获取关注用户ID列表失败", e);
        }

        return followIds;
    }

    @Override
    public List<Long> getFollowerUserIds(Connection conn, Long userId) {
        String sql = "SELECT user_id FROM user_follow WHERE follow_id = ?";
        List<Long> followerIds = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                followerIds.add(rs.getLong("user_id"));
            }

            LogUtil.logBusiness(logger, "GET_FOLLOWER_USER_IDS",
                    "User " + userId + " has " + followerIds.size() + " followers");
        } catch (SQLException e) {
            LogUtil.logError(logger, "获取粉丝ID列表失败: userId=" + userId, e);
            throw new DatabaseException("获取粉丝ID列表失败", e);
        }

        return followerIds;
    }


    /**
     * 将 ResultSet 映射为 UserFollow 对象
     */
    private UserFollow mapRow(ResultSet rs) throws SQLException {
        UserFollow follow = new UserFollow();
        follow.setId(rs.getLong("id"));
        follow.setUserId(rs.getLong("user_id"));
        follow.setFollowId(rs.getLong("follow_id"));
        follow.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return follow;
    }
}
