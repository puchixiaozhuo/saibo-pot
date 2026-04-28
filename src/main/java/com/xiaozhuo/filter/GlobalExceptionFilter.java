package com.xiaozhuo.filter;

import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.util.LogUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * 全局异常过滤器：捕获所有 Servlet 的未处理异常
 */
@WebFilter("/*")
public class GlobalExceptionFilter implements Filter {

    private static final Logger logger = LogUtil.getLogger(GlobalExceptionFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("GlobalExceptionFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.warning("Exception caught by filter: " + e.getMessage());
            GlobalExceptionHandler.handleException(httpResponse, e, httpRequest.getRequestURI());
        }
    }

    @Override
    public void destroy() {
        logger.info("GlobalExceptionFilter destroyed");
    }
}