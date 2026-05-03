package com.xiaozhuo.util;

import java.util.logging.Logger;

/**
 * 基于 Redis 的分布式令牌桶限流器
 *
 * 原理：
 * - 每个限流维度（IP/用户/接口）维护一个令牌桶
 * - 令牌以固定速率生成，最多累积到桶容量
 * - 请求到来时尝试获取1个令牌
 *   - 有令牌 → 通过
 *   - 无令牌 → 拒绝（触发限流）
 *
 * 优势：
 * - 允许短时突发流量（桶内有存量令牌）
 * - 长期平均速率可控（令牌生成速率）
 * - 分布式友好（Redis作为共享存储）
 */
public class RateLimiter {

    private static final Logger logger = LogUtil.getLogger(RateLimiter.class);

    /**
     * Lua 脚本：原子性地实现令牌桶算法
     *
     * 脚本逻辑：
     * 1. 获取当前桶状态（令牌数、上次补充时间）
     * 2. 计算从上次到现在应该新增的令牌数
     * 3. 更新桶内令牌数（不超过容量）
     * 4. 尝试获取1个令牌
     * 5. 返回是否获取成功
     */
    private static final String TOKEN_BUCKET_LUA_SCRIPT =
        "local key = KEYS[1]\n" +
        "local capacity = tonumber(ARGV[1])\n" +
        "local refillRate = tonumber(ARGV[2])\n" +
        "local now = tonumber(ARGV[3])\n" +
        "\n" +
        "-- 获取当前桶状态\n" +
        "local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')\n" +
        "local tokens = tonumber(bucket[1])\n" +
        "local lastRefill = tonumber(bucket[2])\n" +
        "\n" +
        "-- 初始化桶（首次访问）\n" +
        "if tokens == nil then\n" +
        "    tokens = capacity\n" +
        "    lastRefill = now\n" +
        "end\n" +
        "\n" +
        "-- 计算新增令牌数\n" +
        "local elapsed = math.max(0, now - lastRefill)\n" +
        "local newTokens = math.floor(elapsed * refillRate)\n" +
        "tokens = math.min(capacity, tokens + newTokens)\n" +
        "\n" +
        "-- 尝试获取1个令牌\n" +
        "local acquired = 0\n" +
        "if tokens >= 1 then\n" +
        "    tokens = tokens - 1\n" +
        "    acquired = 1\n" +
        "end\n" +
        "\n" +
        "-- 更新桶状态\n" +
        "redis.call('HMSET', key, 'tokens', tostring(tokens), 'last_refill', tostring(now))\n" +
        "redis.call('EXPIRE', key, 120)\n" +
        "\n" +
        "return acquired";

    /**
     * 尝试获取令牌（阻塞式）
     *
     * @param key Redis键（如：rate:token:ip:192.168.1.100）
     * @param capacity 桶容量（最大突发量）
     * @param refillRate 令牌补充速率（每秒多少个）
     * @return true-获取成功（通过限流），false-获取失败（被限流）
     */
    public static boolean tryAcquire(String key, int capacity, int refillRate) {
        try {
            long now = System.currentTimeMillis() / 1000; // 秒级时间戳

            Object result = RedisUtil.eval(
                TOKEN_BUCKET_LUA_SCRIPT,
                key,
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(now)
            );

            boolean acquired = result != null && ((Long) result) == 1L;

            if (!acquired) {
                logger.warning("Rate limit exceeded for key: " + key +
                             ", capacity=" + capacity +
                             ", refillRate=" + refillRate);
            }

            return acquired;

        } catch (Exception e) {
            logger.severe("Rate limiter error for key: " + key + ", error: " + e.getMessage());
            e.printStackTrace();

            // 降级策略：Redis异常时放行请求，避免影响业务
            // 生产环境可根据需求改为拒绝请求
            return true;
        }
    }

    /**
     * 批量检查限流（同时检查多个维度）
     *
     * @param checks 限流检查列表，每个元素包含 [key, capacity, refillRate]
     * @return true-所有检查都通过，false-任一检查失败
     */
    public static boolean tryAcquireMulti(Object[][] checks) {
        for (Object[] check : checks) {
            String key = (String) check[0];
            int capacity = (Integer) check[1];
            int refillRate = (Integer) check[2];

            if (!tryAcquire(key, capacity, refillRate)) {
                return false; // 任一维度被限流，立即返回
            }
        }
        return true;
    }

    /**
     * 重置限流器（测试用）
     *
     * @param key Redis键
     */
    public static void reset(String key) {
        try {
            RedisUtil.del(key);
            logger.info("Rate limiter reset for key: " + key);
        } catch (Exception e) {
            logger.warning("Failed to reset rate limiter: " + e.getMessage());
        }
    }

    /**
     * 获取当前桶内令牌数（监控用）
     *
     * @param key Redis键
     * @return 当前令牌数，-1表示获取失败
     */
    public static int getTokens(String key) {
        try {
            String tokens = RedisUtil.hget(key, "tokens");
            return tokens != null ? Integer.parseInt(tokens) : -1;
        } catch (Exception e) {
            logger.warning("Failed to get tokens for key: " + key);
            return -1;
        }
    }
}
