package com.xiaozhuo.filter;

import com.xiaozhuo.util.RateLimiter;
import com.xiaozhuo.util.LogUtil;
import com.xiaozhuo.util.TokenUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * 全局限流过滤器
 *
 * 限流维度：
 * 1. IP级别：防止单个IP发起过多请求（防DDoS）
 * 2. 用户级别：防止单个用户账号滥用接口
 * 3. 接口级别：保护敏感接口（如优惠券抢购）
 *
 * 限流算法：令牌桶（Token Bucket）
 * - 允许短时突发流量
 * - 长期平均速率可控
 * - 分布式友好（基于Redis）
 */
public class RateLimitFilter implements Filter {

    private static final Logger logger = LogUtil.getLogger(RateLimitFilter.class);

    // ==================== 限流配置 ====================

    /**
     * IP级别限流配置
     * - 桶容量：60个令牌
     * - 补充速率：每秒1个令牌
     * - 效果：平均每分钟60次请求，允许短时burst到60次
     */
    private static final int IP_BUCKET_CAPACITY = 60;
    private static final int IP_REFILL_RATE = 1;

    /**
     * 用户级别限流配置
     * - 桶容量：10个令牌
     * - 补充速率：每秒10个令牌
     * - 效果：平均每秒10次请求，允许短时burst到10次
     */
    private static final int USER_BUCKET_CAPACITY = 10;
    private static final int USER_REFILL_RATE = 10;

    /**
     * 抢购接口限流配置
     * - 桶容量：1000个令牌
     * - 补充速率：每秒1000个令牌
     * - 效果：保护高并发抢购场景
     */
    private static final int COUPON_GRAB_BUCKET_CAPACITY = 1000;
    private static final int COUPON_GRAB_REFILL_RATE = 1000;

    /**
     * 登录接口限流配置（防暴力破解）
     * - 桶容量：5个令牌
     * - 补充速率：每秒1个令牌
     * - 效果：每分钟最多5次登录尝试
     */
    private static final int LOGIN_BUCKET_CAPACITY = 5;
    private static final int LOGIN_REFILL_RATE = 1;

    // ==================== 白名单路径 ====================

    /**
     * 不限流的路径（静态资源、健康检查等）
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/index.html",
        "/favicon.ico",
        "/static/",
        "/css/",
        "/js/",
        "/images/",
        "/health"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RateLimitFilter initialized with Token Bucket algorithm");
        logger.info("IP limit: " + IP_BUCKET_CAPACITY + " requests/min");
        logger.info("User limit: " + USER_BUCKET_CAPACITY + " requests/sec");
        logger.info("Coupon grab limit: " + COUPON_GRAB_BUCKET_CAPACITY + " requests/sec");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String method = req.getMethod();

        // 去除上下文路径
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }

        // 跳过 OPTIONS 预检请求（不消耗令牌）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否在白名单中
        if (isExcluded(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(req);

        // ==================== 第1层：IP级别限流 ====================
        String ipKey = "rate:token:ip:" + clientIp;
        if (!RateLimiter.tryAcquire(ipKey, IP_BUCKET_CAPACITY, IP_REFILL_RATE)) {
            logger.warning("IP rate limit exceeded for: " + clientIp);
            handleRateLimitExceeded(resp, "Too many requests from your IP. Please try again later.");
            return;
        }

        // ==================== 第2层：特殊接口限流 ====================

        // 登录接口限流（防暴力破解）
        if (uri.startsWith("/api/user/login")) {
            String loginKey = "rate:token:login:" + clientIp;
            if (!RateLimiter.tryAcquire(loginKey, LOGIN_BUCKET_CAPACITY, LOGIN_REFILL_RATE)) {
                logger.warning("Login rate limit exceeded for IP: " + clientIp);
                handleRateLimitExceeded(resp, "Too many login attempts. Please try again in 1 minute.");
                return;
            }
        }

        // 抢购接口限流
        if (uri.contains("/coupon/") && uri.contains("/grab")) {
            String couponKey = "rate:token:coupon:grab";
            if (!RateLimiter.tryAcquire(couponKey, COUPON_GRAB_BUCKET_CAPACITY, COUPON_GRAB_REFILL_RATE)) {
                logger.warning("Coupon grab rate limit exceeded");
                handleRateLimitExceeded(resp, "System is busy. Please try again later.");
                return;
            }
        }

        // ==================== 第3层：用户级别限流 ====================
        Long userId = getUserIdFromRequest(req);

        if (userId != null) {
            String userKey = "rate:token:user:" + userId;
            if (!RateLimiter.tryAcquire(userKey, USER_BUCKET_CAPACITY, USER_REFILL_RATE)) {
                logger.warning("User rate limit exceeded for userId: " + userId);
                handleRateLimitExceeded(resp, "Too many requests. Please slow down.");
                return;
            }
        }

        // 所有检查通过，继续处理请求
        chain.doFilter(request, response);
    }

    /**
     * 从请求中获取 userId
     * 优先从 request attribute 获取（AuthFilter 已设置）
     * 如果不存在，尝试从 Token 中解析
     */
    private Long getUserIdFromRequest(HttpServletRequest req) {
        // 1. 优先使用 AuthFilter 设置的 attribute
        Long userId = (Long) req.getAttribute("userId");
        if (userId != null) {
            return userId;
        }

        // 2. 尝试从 Authorization Header 中解析
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                userId = TokenUtil.parseUserId(token);
                if (userId != null) {
                    logger.fine("Parsed userId from token: " + userId);
                }
            } catch (Exception e) {
                logger.fine("Failed to parse userId from token: " + e.getMessage());
            }
        }

        return userId;
    }

    /**
     * 处理限流超限响应
     */
    private void handleRateLimitExceeded(HttpServletResponse resp, String message)
            throws IOException {
        resp.setStatus(429); // HTTP 429 Too Many Requests
        resp.setContentType("application/json;charset=UTF-8");

        // 添加标准限流响应头
        resp.setHeader("Retry-After", "30"); // 建议30秒后重试
        resp.setHeader("X-RateLimit-Limit", "See API documentation");
        resp.setHeader("X-RateLimit-Remaining", "0");

        String jsonResponse = String.format(
            "{\"code\":429,\"message\":\"%s\",\"data\":null}",
            message.replace("\"", "\\\"")
        );

        resp.getWriter().write(jsonResponse);

        logger.warning("Request rate limited: " + message);
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isExcluded(String uri) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (uri.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端真实IP
     * 考虑反向代理场景（Nginx、CDN等）
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先从代理头获取
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        // 如果还是没有，使用远程地址
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个IP（客户端, 代理1, 代理2...）
        // 取第一个（客户端IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    @Override
    public void destroy() {
        logger.info("RateLimitFilter destroyed");
    }
}
