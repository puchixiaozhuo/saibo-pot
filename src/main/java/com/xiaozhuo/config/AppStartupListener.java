package com.xiaozhuo.config;

import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.util.LogUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * 应用启动监听器：初始化 IoC 容器和全局组件
 */
@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger logger = LogUtil.getLogger(AppStartupListener.class);

    public static ApplicationContext applicationContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("========== Application Starting ==========");

        try {
            // 1. 初始化 IoC 容器
            applicationContext = new ApplicationContext();
            applicationContext.init("com.xiaozhuo");

            logger.info("IoC Container initialized with " +
                      applicationContext.getBeanCount() + " beans");

            // 2. 将 ApplicationContext 存储到 ServletContext
            sce.getServletContext().setAttribute("applicationContext", applicationContext);

            logger.info("========== Application Started Successfully ==========");

        } catch (Exception e) {
            LogUtil.logError(logger, "Application startup failed", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("========== Application Shutting Down ==========");

        if (applicationContext != null) {
            applicationContext.destroy();
        }

        logger.info("========== Application Stopped ==========");
    }
}