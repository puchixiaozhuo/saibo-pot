package com.xiaozhuo.service.Impl;

import com.xiaozhuo.bean.vo.FollowVO;
import com.xiaozhuo.dao.FollowDao;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.impl.FollowDaoImpl;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.entity.UserFollow;
import com.xiaozhuo.service.FollowService;
import com.xiaozhuo.util.JDBCUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;

public class FollowServiceImpl implements FollowService {

    private FollowDao followDao = new FollowDaoImpl();
    private UserDao userDao = new UserDaoImpl();

        @Override
    public Map<String, Object> followUser(Long userId, Long followId) {
        Map<String, Object> result = new HashMap<>();

        if (userId.equals(followId)) {
            result.put("code", 400);
            result.put("message", "不能关注自己");
            return result;
        }

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            conn.setAutoCommit(false);

            UserFollow existFollow = followDao.selectByUserIdAndFollowId(conn, userId, followId);
            if (existFollow != null) {
                result.put("code", 400);
                result.put("message", "已经关注过该用户");
                conn.rollback();
                return result;
            }

            User targetUser = userDao.findById(conn, followId);
            if (targetUser == null) {
                result.put("code", 404);
                result.put("message", "目标用户不存在");
                conn.rollback();
                return result;
            }

            UserFollow userFollow = new UserFollow();
            userFollow.setUserId(userId);
            userFollow.setFollowId(followId);
            userFollow.setCreateTime(LocalDateTime.now());

            int rows = followDao.insert(conn, userFollow);
            if (rows > 0) {
                conn.commit();
                result.put("code", 200);
                result.put("message", "关注成功");
                result.put("data", true);
            } else {
                conn.rollback();
                result.put("code", 500);
                result.put("message", "关注失败");
            }
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }
        return result;
    }
// ... existing code ...
    @Override
    public Map<String, Object> unfollowUser(Long userId, Long followId) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            conn.setAutoCommit(false);

            UserFollow existFollow = followDao.selectByUserIdAndFollowId(conn, userId, followId);
            if (existFollow == null) {
                result.put("code", 400);
                result.put("message", "未关注该用户");
                conn.rollback();
                return result;
            }

            int rows = followDao.deleteByUserIdAndFollowId(conn, userId, followId);
            if (rows > 0) {
                conn.commit();
                result.put("code", 200);
                result.put("message", "取消关注成功");
                result.put("data", true);
            } else {
                conn.rollback();
                result.put("code", 500);
                result.put("message", "取消关注失败");
            }
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }
        return result;
    }
// ... existing code ...
    @Override
    public Map<String, Object> getFollowingList(Long userId, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            List<UserFollow> followingList = followDao.selectFollowingList(conn, userId, pageNum, pageSize);
            int total = followDao.countFollowing(conn, userId);

            List<FollowVO> voList = new ArrayList<>();
            for (UserFollow follow : followingList) {
                FollowVO vo = convertToFollowVO(conn, follow.getUserId(), follow.getFollowId());
                voList.add(vo);
            }

            result.put("code", 200);
            result.put("message", "查询成功");
            Map<String, Object> data = new HashMap<>();
            data.put("list", voList);
            data.put("total", total);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }
        return result;
    }
// ... existing code ...
    @Override
    public Map<String, Object> getFollowerList(Long userId, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            List<UserFollow> followerList = followDao.selectFollowerList(conn, userId, pageNum, pageSize);
            int total = followDao.countFollower(conn, userId);

            List<FollowVO> voList = new ArrayList<>();
            for (UserFollow follow : followerList) {
                FollowVO vo = convertToFollowVO(conn, follow.getUserId(), follow.getFollowId());
                voList.add(vo);
            }

            result.put("code", 200);
            result.put("message", "查询成功");
            Map<String, Object> data = new HashMap<>();
            data.put("list", voList);
            data.put("total", total);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }
        return result;
    }
// ... existing code ...
    @Override
    public Map<String, Object> getFollowStatus(Long userId, Long targetUserId) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            boolean isFollowing = followDao.isFollowing(conn, userId, targetUserId);
            result.put("code", 200);
            result.put("message", "查询成功");
            Map<String, Object> data = new HashMap<>();
            data.put("isFollowing", isFollowing);
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }
        return result;
    }
// ... existing code ...
    @Override
    public Map<String, Object> getFollowCounts(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();
            int followingCount = followDao.countFollowing(conn, userId);
            int followerCount = followDao.countFollower(conn, userId);

            result.put("code", 200);
            result.put("message", "查询成功");
            Map<String, Object> data = new HashMap<>();
            data.put("followingCount", followingCount);
            data.put("followerCount", followerCount);
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "服务器错误：" + e.getMessage());
        } finally {
            JDBCUtil.close(conn, null);
        }
        return result;
    }
// ... existing code ...


    private FollowVO convertToFollowVO(Connection conn, Long currentUserId, Long targetUserId) throws Exception {
        User targetUser = userDao.findById(conn, targetUserId);
        FollowVO vo = new FollowVO();
        vo.setId(targetUserId);
        vo.setUserId(targetUserId);
        vo.setUserName(targetUser.getUsername());
        vo.setUserAvatar(targetUser.getAvatar());
        vo.setNickname(targetUser.getNickname());
        vo.setFollowerCount((long) followDao.countFollower(conn, targetUserId));
        vo.setFollowingCount((long) followDao.countFollowing(conn, targetUserId));
        vo.setIsFollowed(followDao.isFollowing(conn, currentUserId, targetUserId));
        return vo;
    }
}
