package com.xiaozhuo.annotation;

import java.lang.annotation.*;

/**
 * 事务注解：标记需要事务管理的方法
 * 可用于 Service 层方法，实现声明式事务管理
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {

    /**
     * 事务传播行为（预留，当前仅支持 REQUIRED）
     * REQUIRED: 如果当前存在事务，则加入该事务；否则创建新事务
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * 超时时间（秒），默认 30 秒
     */
    int timeout() default 30;

    /**
     * 是否只读事务（预留）
     */
    boolean readOnly() default false;

    /**
     * 事务传播行为枚举
     */
    enum Propagation {
        REQUIRED,      // 默认：需要事务
        REQUIRES_NEW,  // 创建新事务（预留）
        SUPPORTS       // 支持事务（预留）
    }
}