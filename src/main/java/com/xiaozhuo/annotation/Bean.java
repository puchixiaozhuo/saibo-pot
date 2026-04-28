package com.xiaozhuo.annotation;

import java.lang.annotation.*;

/**
 * Bean 注解：标记需要由 IoC 容器管理的类
 * 被此注解标记的类会自动注册到 ApplicationContext 中
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

    /**
     * Bean 的名称（可选）
     * 如果不指定，默认使用类名首字母小写
     * @return Bean 名称
     */
    String value() default "";

    /**
     * Bean 的作用域
     * @return 作用域类型
     */
    BeanScope scope() default BeanScope.SINGLETON;
}