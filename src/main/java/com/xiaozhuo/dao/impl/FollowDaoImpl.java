package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.FollowDao;
import com.xiaozhuo.entity.UserFollow;
import com.xiaozhuo.util.JDBCUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FollowDaoImpl implements FollowDao {

    @Override
    public int insert(Connection conn, UserFollow userFollow) {
        String sql = "INSERT INTO user_follow (user_id, follow_id, create_time) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userFollow.getUserId());
            ps.setLong(2, userFollow.getFollowId());
            ps.setTimestamp(3, Timestamp.valueOf(userFollow.getCreateTime()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int deleteByUserIdAndFollowId(Connection conn, Long userId, Long followId) {
        String sql = "DELETE FROM user_follow WHERE user_id = ? AND follow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, followId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public UserFollow selectByUserIdAndFollowId(Connection conn, Long userId, Long followId) {
        String sql = "SELECT * FROM user_follow WHERE user_id = ? AND follow_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, followId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UserFollow follow = new UserFollow();
                follow.setId(rs.getLong("id"));
                follow.setUserId(rs.getLong("user_id"));
                follow.setFollowId(rs.getLong("follow_id"));
                follow.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                return follow;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<UserFollow> selectFollowingList(Connection conn, Long userId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM user_follow WHERE user_id = ? ORDER BY create_time DESC LIMIT ?, ?";
        List<UserFollow> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, (pageNum - 1) * pageSize);
            ps.setInt(3, pageSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UserFollow follow = new UserFollow();
                follow.setId(rs.getLong("id"));
                follow.setUserId(rs.getLong("user_id"));
                follow.setFollowId(rs.getLong("follow_id"));
                follow.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                list.add(follow);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<UserFollow> selectFollowerList(Connection conn, Long followId, int pageNum, int pageSize) {
        String sql = "SELECT * FROM user_follow WHERE follow_id = ? ORDER BY create_time DESC LIMIT ?, ?";
        List<UserFollow> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, followId);
            ps.setInt(2, (pageNum - 1) * pageSize);
            ps.setInt(3, pageSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UserFollow follow = new UserFollow();
                follow.setId(rs.getLong("id"));
                follow.setUserId(rs.getLong("user_id"));
                follow.setFollowId(rs.getLong("follow_id"));
                follow.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                list.add(follow);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

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
            e.printStackTrace();
        }
        return 0;
    }

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
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean isFollowing(Connection conn, Long userId, Long followId) {
        return selectByUserIdAndFollowId(conn, userId, followId) != null;
    }
}
