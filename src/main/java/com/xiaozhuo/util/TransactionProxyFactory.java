package com.xiaozhuo.util;

import com.xiaozhuo.annotation.Transactional;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * 事务代理工厂：使用 JDK 动态代理实现声明式事务管理
 * 通过拦截带有 @Transactional 注解的方法，自动管理事务
 */
public class TransactionProxyFactory {

    private static final Logger logger = LogUtil.getLogger(TransactionProxyFactory.class);

    /**
     * 创建事务代理对象
     * 对目标对象进行包装，拦截带有 @Transactional 注解的方法
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        Class<?> targetClass = target.getClass();

        if (targetClass.getInterfaces().length > 0) {
            logger.info("Creating transaction proxy for: " + targetClass.getSimpleName());

            return (T) Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                targetClass.getInterfaces(),
                new TransactionInvocationHandler(target)
            );
        } else {
            logger.warning("Target class [" + targetClass.getSimpleName() +
                         "] does not implement interfaces, returning original object");
            return target;
        }
    }

    /**
     * 事务调用处理器：拦截方法调用，处理事务逻辑
     */
    private static class TransactionInvocationHandler implements InvocationHandler {

        private final Object target;

        public TransactionInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if ("toString".equals(methodName) || "hashCode".equals(methodName) || "equals".equals(methodName)) {
                return method.invoke(target, args);
            }

            if (method.isAnnotationPresent(Transactional.class)) {
                Transactional transactional = method.getAnnotation(Transactional.class);
                LogUtil.logTransaction(logger, "Method [" + methodName + "] is annotated with @Transactional");

                try {
                    TransactionManager.beginTransaction();

                    Object result = method.invoke(target, args);

                    TransactionManager.commit();

                    LogUtil.logTransaction(logger, "Method [" + methodName + "] executed successfully");
                    return result;

                } catch (Throwable e) {
                    TransactionManager.rollback();
                    LogUtil.logError(logger, "Method [" + methodName + "] failed, transaction rolled back", e);

                    Throwable cause = e.getCause();
                    if (cause != null) {
                        throw cause;
                    } else {
                        throw e;
                    }
                }
            } else {
                return method.invoke(target, args);
            }
        }
    }
}
