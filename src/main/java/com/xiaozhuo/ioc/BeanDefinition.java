package com.xiaozhuo.ioc;

import com.xiaozhuo.annotation.BeanScope;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean 定义：存储 Bean 的元数据信息
 */
public class BeanDefinition {

    // Bean 名称
    private String beanName;

    // Bean 类型
    private Class<?> beanClass;

    // 作用域
    private BeanScope scope;

    // 构造函数
    private Constructor<?> constructor;

    // 依赖的 Bean 名称列表
    private List<String> dependencies = new ArrayList<>();

    // Bean 实例（单例模式下使用）
    private Object instance;

    // 是否已经初始化
    private boolean initialized = false;

    public BeanDefinition(String beanName, Class<?> beanClass, BeanScope scope) {
        this.beanName = beanName;
        this.beanClass = beanClass;
        this.scope = scope;

        // 获取构造函数
        try {
            this.constructor = beanClass.getDeclaredConstructor();
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No default constructor found for: " + beanClass.getName(), e);
        }
    }

    // Getters and Setters
    public String getBeanName() {
        return beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public BeanScope getScope() {
        return scope;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "beanName='" + beanName + '\'' +
                ", beanClass=" + beanClass.getSimpleName() +
                ", scope=" + scope +
                ", initialized=" + initialized +
                '}';
    }
}