package com.xiaozhuo.factory;

import com.xiaozhuo.util.TransactionProxyFactory;
import com.xiaozhuo.util.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service 工厂：统一管理 Service 实例的创建
 * 自动为 Service 实例添加事务代理支持
 */
public class ServiceFactory {

    private static final Logger logger = LogUtil.getLogger(ServiceFactory.class);

    // 单例缓存：存储已创建的 Service 实例
    private static final Map<String, Object> serviceCache = new HashMap<>();

    /**
     * 获取 UserService 实例（带事务代理）
     * @return UserService 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getUserService() {
        return (T) getService("userService", () -> {
            try {
                Class<?> implClass = Class.forName("com.xiaozhuo.service.Impl.UserServiceImpl");
                Object instance = implClass.getDeclaredConstructor().newInstance();
                return TransactionProxyFactory.createProxy(instance);
            } catch (Exception e) {
                LogUtil.logError(logger, "Failed to create UserService", e);
                throw new RuntimeException("Failed to create UserService", e);
            }
        });
    }

    /**
     * 获取 VideoService 实例（带事务代理）
     * @return VideoService 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getVideoService() {
        return (T) getService("videoService", () -> {
            try {
                Class<?> implClass = Class.forName("com.xiaozhuo.service.Impl.VideoServiceImpl");
                Object instance = implClass.getDeclaredConstructor().newInstance();
                return TransactionProxyFactory.createProxy(instance);
            } catch (Exception e) {
                LogUtil.logError(logger, "Failed to create VideoService", e);
                throw new RuntimeException("Failed to create VideoService", e);
            }
        });
    }

    /**
     * 获取 CommentService 实例（带事务代理）
     * @return CommentService 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCommentService() {
        return (T) getService("commentService", () -> {
            try {
                Class<?> implClass = Class.forName("com.xiaozhuo.service.Impl.CommentServiceImpl");
                Object instance = implClass.getDeclaredConstructor().newInstance();
                return TransactionProxyFactory.createProxy(instance);
            } catch (Exception e) {
                LogUtil.logError(logger, "Failed to create CommentService", e);
                throw new RuntimeException("Failed to create CommentService", e);
            }
        });
    }

    /**
     * 获取 FollowService 实例（带事务代理）
     * @return FollowService 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFollowService() {
        return (T) getService("followService", () -> {
            try {
                Class<?> implClass = Class.forName("com.xiaozhuo.service.Impl.FollowServiceImpl");
                Object instance = implClass.getDeclaredConstructor().newInstance();
                return TransactionProxyFactory.createProxy(instance);
            } catch (Exception e) {
                LogUtil.logError(logger, "Failed to create FollowService", e);
                throw new RuntimeException("Failed to create FollowService", e);
            }
        });
    }

    /**
     * 获取 PermissionService 实例（带事务代理）
     * @return PermissionService 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPermissionService() {
        return (T) getService("permissionService", () -> {
            try {
                Class<?> implClass = Class.forName("com.xiaozhuo.service.Impl.PermissionServiceImpl");
                Object instance = implClass.getDeclaredConstructor().newInstance();
                return TransactionProxyFactory.createProxy(instance);
            } catch (Exception e) {
                LogUtil.logError(logger, "Failed to create PermissionService", e);
                throw new RuntimeException("Failed to create PermissionService", e);
            }
        });
    }

    /**
     * 通用 Service 获取方法（带缓存）
     * @param key 缓存键
     * @param supplier Service 创建器
     * @return Service 实例（可能是代理对象）
     */
    @SuppressWarnings("unchecked")
    private static <T> T getService(String key, ServiceSupplier<T> supplier) {
        if (!serviceCache.containsKey(key)) {
            synchronized (ServiceFactory.class) {
                if (!serviceCache.containsKey(key)) {
                    T service = supplier.get();
                    serviceCache.put(key, service);
                    logger.info("Created and cached service: " + key);
                }
            }
        }
        return (T) serviceCache.get(key);
    }

    /**
     * 函数式接口：Service 创建器
     */
    @FunctionalInterface
    private interface ServiceSupplier<T> {
        T get();
    }

    /**
     * 清除缓存（用于测试或重新加载）
     */
    public static void clearCache() {
        serviceCache.clear();
        logger.info("Service cache cleared");
    }
}