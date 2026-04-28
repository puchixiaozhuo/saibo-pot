package com.xiaozhuo.ioc;

import com.xiaozhuo.annotation.Autowired;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.BeanScope;
import com.xiaozhuo.util.LogUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ApplicationContext：IoC 容器核心实现
 * 负责 Bean 的注册、创建、管理和依赖注入
 */
public class ApplicationContext {

    private static final Logger logger = LogUtil.getLogger(ApplicationContext.class);

    // Bean 定义缓存：beanName -> BeanDefinition
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();

    // 单例 Bean 缓存：beanName -> Instance
    private final Map<String, Object> singletonBeans = new ConcurrentHashMap<>();

    // 正在创建的 Bean（用于检测循环依赖）
    private final ThreadLocal<List<String>> creatingBeans = ThreadLocal.withInitial(ArrayList::new);

    // 是否已经初始化
    private boolean initialized = false;

    /**
     * 初始化 IoC 容器
     * 扫描指定包下的所有类，注册带有 @Bean 注解的类
     *
     * @param basePackage 基础包名
     */
    public void init(String basePackage) {
        if (initialized) {
            logger.warning("ApplicationContext already initialized");
            return;
        }

        logger.info("Initializing ApplicationContext, scanning package: " + basePackage);

        try {
            // 扫描包并注册 Bean
            scanAndRegisterBeans(basePackage);

            // 实例化所有单例 Bean
            instantiateSingletons();

            initialized = true;
            logger.info("ApplicationContext initialized successfully with " +
                      beanDefinitions.size() + " beans");

        } catch (Exception e) {
            LogUtil.logError(logger, "Failed to initialize ApplicationContext", e);
            throw new RuntimeException("ApplicationContext initialization failed", e);
        }
    }

    /**
     * 扫描包并注册 Bean
     */
    private void scanAndRegisterBeans(String basePackage) throws Exception {
        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(path);

        if (url == null) {
            logger.warning("Package not found: " + basePackage);
            return;
        }

        File directory = new File(url.getFile());
        if (!directory.exists()) {
            logger.warning("Directory not found: " + directory.getPath());
            return;
        }

        scanDirectory(directory, basePackage);
    }

    /**
     * 递归扫描目录
     */
    private void scanDirectory(File directory, String packageName) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                // 处理类文件
                String className = packageName + '.' + file.getName().replace(".class", "");
                processClass(className);
            }
        }
    }

    /**
     * 处理类：检查是否有 @Bean 注解，如果有则注册
     */
    private void processClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            // 检查是否有 @Bean 注解
            if (clazz.isAnnotationPresent(Bean.class)) {
                Bean beanAnnotation = clazz.getAnnotation(Bean.class);

                // 确定 Bean 名称
                String beanName = beanAnnotation.value();
                if (beanName.isEmpty()) {
                    // 默认使用类名首字母小写
                    beanName = uncapitalize(clazz.getSimpleName());
                }

                // 创建 BeanDefinition
                BeanScope scope = beanAnnotation.scope();
                BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, scope);

                // 注册 Bean
                beanDefinitions.put(beanName, beanDefinition);
                logger.info("Registered bean: " + beanName + " [" + clazz.getSimpleName() + "]");

                // 分析依赖
                analyzeDependencies(beanDefinition);
            }

        } catch (ClassNotFoundException e) {
            LogUtil.logError(logger, "Failed to load class: " + className, e);
        }
    }

    /**
     * 分析 Bean 的依赖关系
     */
    private void analyzeDependencies(BeanDefinition beanDefinition) {
        Class<?> clazz = beanDefinition.getBeanClass();

        // 检查字段上的 @Autowired
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                String dependencyName = uncapitalize(field.getType().getSimpleName());
                beanDefinition.addDependency(dependencyName);
                logger.fine("Bean [" + beanDefinition.getBeanName() +
                          "] depends on [" + dependencyName + "]");
            }
        }
    }

    /**
     * 实例化所有单例 Bean
     */
    private void instantiateSingletons() {
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            if (beanDefinition.getScope() == BeanScope.SINGLETON) {
                getBean(beanDefinition.getBeanName());
            }
        }
    }

    /**
     * 获取 Bean 实例
     * @param beanName Bean 名称
     * @return Bean 实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitions.get(beanName);

        if (beanDefinition == null) {
            throw new RuntimeException("Bean not found: " + beanName);
        }

        // 单例模式：从缓存中获取
        if (beanDefinition.getScope() == BeanScope.SINGLETON) {
            if (singletonBeans.containsKey(beanName)) {
                return (T) singletonBeans.get(beanName);
            }
        }

        // 检测循环依赖
        if (creatingBeans.get().contains(beanName)) {
            throw new RuntimeException("Circular dependency detected: " + creatingBeans.get());
        }

        // 创建 Bean 实例
        return (T) createBean(beanDefinition);
    }

    /**
     * 根据类型获取 Bean
     * 支持通过接口类型查找实现类
     * @param beanType Bean 类型（可以是接口或实现类）
     * @return Bean 实例
     */
    public <T> T getBean(Class<T> beanType) {
        // 先尝试直接通过类名查找
        String beanName = uncapitalize(beanType.getSimpleName());

        if (beanDefinitions.containsKey(beanName)) {
            return getBean(beanName);
        }

        // 如果是接口，查找实现了该接口的 Bean
        if (beanType.isInterface()) {
            for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
                Class<?> beanClass = entry.getValue().getBeanClass();
                if (beanType.isAssignableFrom(beanClass)) {
                    logger.fine("Found implementation [" + entry.getKey() +
                            "] for interface [" + beanType.getSimpleName() + "]");
                    return getBean(entry.getKey());
                }
            }
        }

        throw new RuntimeException("Bean not found for type: " + beanType.getName());
    }

    /**
     * 创建 Bean 实例
     */
    private Object createBean(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getBeanName();

        try {
            // 标记正在创建
            creatingBeans.get().add(beanName);

            logger.info("Creating bean: " + beanName);

            // 创建实例
            Object instance = beanDefinition.getConstructor().newInstance();

            // 依赖注入
            injectDependencies(instance, beanDefinition);

            // 如果是单例，放入缓存
            if (beanDefinition.getScope() == BeanScope.SINGLETON) {
                singletonBeans.put(beanName, instance);
                beanDefinition.setInstance(instance);
            }

            beanDefinition.setInitialized(true);
            logger.info("Bean created successfully: " + beanName);

            return instance;

        } catch (Exception e) {
            LogUtil.logError(logger, "Failed to create bean: " + beanName, e);
            throw new RuntimeException("Failed to create bean: " + beanName, e);
        } finally {
            creatingBeans.get().remove(beanName);
        }
    }

    /**
     * 注入依赖
     */
    private void injectDependencies(Object instance, BeanDefinition beanDefinition) {
        Class<?> clazz = instance.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Autowired autowired = field.getAnnotation(Autowired.class);
                String dependencyName = uncapitalize(field.getType().getSimpleName());

                try {
                    // 获取依赖的 Bean
                    Object dependency = getBean(dependencyName);

                    // 注入字段
                    field.setAccessible(true);
                    field.set(instance, dependency);

                    logger.fine("Injected dependency [" + dependencyName +
                              "] into bean [" + beanDefinition.getBeanName() + "]");

                } catch (Exception e) {
                    if (autowired.required()) {
                        LogUtil.logError(logger, "Failed to inject required dependency: " +
                                       dependencyName, e);
                        throw new RuntimeException("Failed to inject dependency: " +
                                                 dependencyName, e);
                    } else {
                        logger.warning("Failed to inject optional dependency: " + dependencyName);
                    }
                }
            }
        }
    }

    /**
     * 检查是否包含指定的 Bean
     * @param beanName Bean 名称
     * @return true-存在，false-不存在
     */
    public boolean containsBean(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    /**
     * 获取所有 Bean 名称
     * @return Bean 名称列表
     */
    public String[] getBeanDefinitionNames() {
        return beanDefinitions.keySet().toArray(new String[0]);
    }

    /**
     * 获取 Bean 数量
     * @return Bean 数量
     */
    public int getBeanCount() {
        return beanDefinitions.size();
    }

    /**
     * 销毁容器（关闭应用时调用）
     */
    public void destroy() {
        logger.info("Destroying ApplicationContext...");

        singletonBeans.clear();
        beanDefinitions.clear();
        creatingBeans.remove();

        logger.info("ApplicationContext destroyed");
    }

    /**
     * 首字母小写
     */
    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}