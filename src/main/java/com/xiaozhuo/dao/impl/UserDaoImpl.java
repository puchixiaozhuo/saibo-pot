package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.entity.User;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 用户数据访问实现类
 */
public class UserDaoImpl extends BaseDaoImpl<User> implements UserDao {

    private static final Logger logger = LogUtil.getLogger(UserDaoImpl.class);

    public UserDaoImpl() {
        super(User.class);
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public User findByUsername(Connection conn, String username) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("username", username);

        try {
            List<User> users = findByCondition(conn, conditions);
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            LogUtil.logError(logger, "根据用户名查询用户失败: username=" + username, e);
            throw new DatabaseException("查询用户失败", e);
        }
    }

    /**
     * 插入新用户
     */
    @Override
    public int insert(Connection conn, User user) {
        try {
            return super.insert(conn, user);
        } catch (Exception e) {
            LogUtil.logError(logger, "插入用户失败: username=" + user.getUsername(), e);
            throw new DatabaseException("插入用户失败", e);
        }
    }

    /**
     * 根据用户 ID 查询用户
     */
    @Override
    public User findById(Connection conn, Long id) {
        try {
            return super.findById(conn, id);
        } catch (Exception e) {
            LogUtil.logError(logger, "根据ID查询用户失败: id=" + id, e);
            throw new DatabaseException("查询用户失败", e);
        }
    }

    /**
     * 更新用户登录时间
     */
    @Override
    public void updateLoginTime(Connection conn, Long userId, LocalDateTime lastLoginTime) {
        try {
            User user = findById(conn, userId);
            if (user != null) {
                user.setUpdateTime(lastLoginTime);
                super.update(conn, user);
            }
        } catch (Exception e) {
            LogUtil.logError(logger, "更新用户登录时间失败: userId=" + userId, e);
            throw new DatabaseException("更新用户登录时间失败", e);
        }
    }
}

