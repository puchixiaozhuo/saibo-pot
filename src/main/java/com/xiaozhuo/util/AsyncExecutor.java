package com.xiaozhuo.util;

import java.util.concurrent.*;

/**
 * 异步任务线程池工具类
 */
public class AsyncExecutor {

    private static final ExecutorService executor = new ThreadPoolExecutor(
        5,                          // 核心线程数
        20,                         // 最大线程数
        60L, TimeUnit.SECONDS,      // 空闲线程存活时间
        new LinkedBlockingQueue<>(1000), // 任务队列
        new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
    );

    /**
     * 提交异步任务
     */
    public static void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        executor.shutdown();
    }
}