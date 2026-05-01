package com.xiaozhuo.config;

import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.task.CouponLotteryTask;
import com.xiaozhuo.util.LogUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger logger = LogUtil.getLogger(AppStartupListener.class);
    public static ApplicationContext applicationContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("========================================");
        System.out.println("Application Starting...");
        System.out.println("========================================");

        // 初始化 IoC 容器
        ApplicationContext context = new ApplicationContext();
        context.init("com.xiaozhuo");
        applicationContext = context;

        ServletContext servletContext = sce.getServletContext();
        servletContext.setAttribute("applicationContext", context);

        logger.info("IoC Container initialized");

        // 🚀 启动优惠券抽签定时任务
        CouponLotteryTask.start();

        System.out.println("========================================");
        System.out.println("Application Started Successfully!");
        System.out.println("========================================");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application Shutting Down...");

        // 🛑 停止定时任务
        CouponLotteryTask.stop();

        logger.info("Application stopped");
    }
}
