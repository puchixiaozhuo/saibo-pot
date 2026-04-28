package com.xiaozhuo.annotation;

import java.lang.annotation.*;

/**
 * Autowired 注解：自动注入依赖
 * 用于字段或 Setter 方法，IoC 容器会自动注入对应的 Bean
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

    /**
     * 是否必须注入
     * true: 如果找不到对应的 Bean 则抛出异常
     * false: 如果找不到对应的 Bean 则忽略
     * @return 是否必须
     */
    boolean required() default true;
}