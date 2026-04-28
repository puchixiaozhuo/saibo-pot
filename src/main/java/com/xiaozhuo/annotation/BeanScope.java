package com.xiaozhuo.annotation;

/**
 * Bean 作用域枚举
 */
public enum BeanScope {

    /**
     * 单例模式：整个应用中只有一个实例
     */
    SINGLETON,

    /**
     * 原型模式：每次获取都创建新实例
     */
    PROTOTYPE
}