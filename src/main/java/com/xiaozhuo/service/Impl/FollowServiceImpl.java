package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.bean.vo.FollowVO;
import com.xiaozhuo.dao.FollowDao;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.impl.FollowDaoImpl;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.entity.UserFollow;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.FollowService;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.TransactionManager;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 关注业务实现类（优化版）
 * 使用声明式事务 + IoC 容器管理
 */
@Bean
public class FollowServiceImpl implements FollowService {

    private FollowDao followDao = new FollowDaoImpl();
    private UserDao userDao = new UserDaoImpl();

    /**
     * 关注用户（声明式事务）
     */
    @Override
    @Transactional
    public Result<Void> followUser(Long userId, Long followId) {
        if (userId == null || followId == null) {
            throw new BusinessException(400, "参数无效");
        }

        if (userId.equals(followId)) {
            throw new BusinessException(400, "不能关注自己");
        }

        Connection conn = TransactionManager.getConnection();

        UserFollow existFollow = followDao.selectByUserIdAndFollowId(conn, userId, followId);
        if (existFollow != null) {
            throw new BusinessException(400, "已经关注过该用户");
        }

        User targetUser = userDao.findById(conn, followId);
        if (targetUser == null) {
            throw new BusinessException(404, "目标用户不存在");
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setUserId(userId);
        userFollow.setFollowId(followId);
        userFollow.setCreateTime(LocalDateTime.now());

        int rows = followDao.insert(conn, userFollow);
        if (rows > 0) {
            return Result.success();
        } else {
            throw new BusinessException(500, "关注失败");
        }
    }

    /**
     * 取消关注用户（声明式事务）
     */
    @Override
    @Transactional
    public Result<Void> unfollowUser(Long userId, Long followId) {
        if (userId == null || followId == null) {
            throw new BusinessException(400, "参数无效");
        }

        Connection conn = TransactionManager.getConnection();

        UserFollow existFollow = followDao.selectByUserIdAndFollowId(conn, userId, followId);
        if (existFollow == null) {
            throw new BusinessException(400, "未关注该用户");
        }

        int rows = followDao.deleteByUserIdAndFollowId(conn, userId, followId);
        if (rows > 0) {
            return Result.success();
        } else {
            throw new BusinessException(500, "取消关注失败");
        }
    }

    /**
     * 获取关注列表
     */
    @Override
    public Result<List<FollowVO>> getFollowingList(Long userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            List<UserFollow> followingList = followDao.selectFollowingList(conn, userId, pageNum, pageSize);
            List<FollowVO> voList = convertToFollowVOList(conn, userId, followingList);

            return Result.success("查询成功", voList);

        } catch (Exception e) {
            throw new BusinessException(500, "查询关注列表失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 获取粉丝列表
     */
    @Override
    public Result<List<FollowVO>> getFollowerList(Long userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            List<UserFollow> followerList = followDao.selectFollowerList(conn, userId, pageNum, pageSize);
            List<FollowVO> voList = convertToFollowVOListReverse(conn, userId, followerList);

            return Result.success("查询成功", voList);

        } catch (Exception e) {
            throw new BusinessException(500, "查询粉丝列表失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 获取关注状态
     */
    @Override
    public Result<Map<String, Object>> getFollowStatus(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            throw new BusinessException(400, "参数无效");
        }

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            boolean isFollowing = followDao.isFollowing(conn, userId, targetUserId);

            Map<String, Object> data = new HashMap<>();
            data.put("isFollowing", isFollowing);

            return Result.success("查询成功", data);

        } catch (Exception e) {
            throw new BusinessException(500, "查询关注状态失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 获取关注数量
     */
    @Override
    public Result<Map<String, Object>> getFollowCounts(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }

        Connection conn = null;
        try {
            conn = TransactionManager.getConnection();

            int followingCount = followDao.countFollowing(conn, userId);
            int followerCount = followDao.countFollower(conn, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("followingCount", followingCount);
            data.put("followerCount", followerCount);

            return Result.success("查询成功", data);

        } catch (Exception e) {
            throw new BusinessException(500, "查询关注数量失败：" + e.getMessage());
        } finally {
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                JDBCUtil.returnToPool(conn);
            }
        }
    }

    /**
     * 将 UserFollow 列表转换为 FollowVO 列表（关注列表）
     */
    private List<FollowVO> convertToFollowVOList(Connection conn, Long currentUserId, List<UserFollow> follows) throws Exception {
        List<FollowVO> voList = new ArrayList<>();

        for (UserFollow follow : follows) {
            FollowVO vo = convertToFollowVO(conn, currentUserId, follow.getFollowId());
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 将 UserFollow 列表转换为 FollowVO 列表（粉丝列表）
     */
    private List<FollowVO> convertToFollowVOListReverse(Connection conn, Long currentUserId, List<UserFollow> follows) throws Exception {
        List<FollowVO> voList = new ArrayList<>();

        for (UserFollow follow : follows) {
            FollowVO vo = convertToFollowVO(conn, currentUserId, follow.getUserId());
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 将单个 UserFollow 转换为 FollowVO
     */
    private FollowVO convertToFollowVO(Connection conn, Long currentUserId, Long targetUserId) throws Exception {
        User targetUser = userDao.findById(conn, targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "用户不存在: " + targetUserId);
        }

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
