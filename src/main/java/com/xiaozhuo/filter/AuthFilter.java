package com.xiaozhuo.filter;

import cn.hutool.core.io.resource.ResourceUtil;
import com.xiaozhuo.service.PermissionService;
import com.xiaozhuo.service.Impl.PermissionServiceImpl;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.ResourcePermissionUtil;
import com.xiaozhuo.util.TokenUtil;
import java.sql.Connection;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/index.html"
    );

    private PermissionService permissionService = new PermissionServiceImpl();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("AuthFilter 初始化成功（含RBAC权限校验）");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        boolean isExcluded = EXCLUDE_PATHS.stream().anyMatch(path::startsWith);

        if (isExcluded) {
            chain.doFilter(request, response);
            return;
        }

        String token = req.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
            return;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            Long userId = TokenUtil.parseUserId(token);
            req.setAttribute("userId", userId);

            if (!checkPermission(req, userId)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"code\":403,\"message\":\"没有权限访问该接口\"}");
                return;
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset.UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
        }
    }

    private boolean checkPermission(HttpServletRequest req, Long userId) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }

        String method = req.getMethod();

        System.out.println("[AuthFilter] ========== Permission Check Start ==========");
        System.out.println("[AuthFilter] URI: " + uri);
        System.out.println("[AuthFilter] Method: " + method);
        System.out.println("[AuthFilter] UserId: " + userId);

        PermissionCheckResult checkResult = mapUriToPermission(uri, method, req);

        System.out.println("[AuthFilter] Required Permission: " + checkResult.requiredPermission);
        System.out.println("[AuthFilter] Need Ownership Check: " + checkResult.needOwnerCheck);

        // Step 1: Check permission if required
        if (checkResult.requiredPermission != null) {
            System.out.println("[AuthFilter] Step 1: Checking specific permission: " + checkResult.requiredPermission);
            boolean hasPermission = permissionService.hasPermission(userId, checkResult.requiredPermission);
            System.out.println("[AuthFilter] Permission result: " + hasPermission);

            if (!hasPermission) {
                System.out.println("[AuthFilter] DENIED: Insufficient permission");
                return false;
            }
            System.out.println("[AuthFilter] PASSED: Has required permission");
        } else {
            System.out.println("[AuthFilter] Step 1: No specific permission required, skip");
        }

        // Step 2: Check ownership if needed
        if (checkResult.needOwnerCheck) {
            System.out.println("[AuthFilter] Step 2: Performing ownership check");
            boolean isOwner = checkResourceOwnership(req, userId);
            System.out.println("[AuthFilter] Ownership check result: " + isOwner);

            if (!isOwner) {
                System.out.println("[AuthFilter] DENIED: Not the resource owner");
                return false;
            }
            System.out.println("[AuthFilter] ALLOWED: Is resource owner");
            return true;
        } else {
            System.out.println("[AuthFilter] Step 2: No ownership check needed");
        }

        System.out.println("[AuthFilter] ALLOWED: All checks passed");
        return true;
    }

    private boolean checkResourceOwnership(HttpServletRequest req, Long userId) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }

        Connection conn = null;

        try {
            System.out.println("[AuthFilter] Ownership Check START");
            System.out.println("[AuthFilter] Original URI: " + req.getRequestURI());
            System.out.println("[AuthFilter] Processed URI: " + uri);
            System.out.println("[AuthFilter] UserId: " + userId);

            conn = JDBCUtil.getConnection();
            System.out.println("[AuthFilter] Database connection established");

            if (uri.startsWith("/api/video/")) {
                String[] parts = uri.split("/");
                Long resourceId = Long.parseLong(parts[parts.length - 1]);
                System.out.println("[AuthFilter] Video resource ID: " + resourceId);

                boolean hasDeleteAnyPermission = permissionService.hasPermission(userId, "video:delete:any");
                System.out.println("[AuthFilter] Has video:delete:any permission: " + hasDeleteAnyPermission);

                if (hasDeleteAnyPermission) {
                    System.out.println("[AuthFilter] ADMIN: Has delete any permission, ALLOWED");
                    return true;
                }

                System.out.println("[AuthFilter] Checking if user is owner...");
                boolean isOwner = ResourcePermissionUtil.isVideoOwner(conn, userId, resourceId);
                System.out.println("[AuthFilter] Is owner result: " + isOwner);

                if (isOwner) {
                    System.out.println("[AuthFilter] OWNER: User owns this video, ALLOWED");
                } else {
                    System.out.println("[AuthFilter] NOT_OWNER: User does not own this video, DENIED");
                }

                return isOwner;

            } else if (uri.startsWith("/api/comment/")) {
                String[] parts = uri.split("/");
                Long resourceId = Long.parseLong(parts[parts.length - 1]);
                System.out.println("[AuthFilter] Comment resource ID: " + resourceId);

                boolean hasDeleteAnyPermission = permissionService.hasPermission(userId, "comment:delete:any");
                System.out.println("[AuthFilter] Has comment:delete:any permission: " + hasDeleteAnyPermission);

                if (hasDeleteAnyPermission) {
                    System.out.println("[AuthFilter] ADMIN: Has delete any comment permission, ALLOWED");
                    return true;
                }

                boolean isOwner = ResourcePermissionUtil.isCommentOwner(conn, userId, resourceId);
                System.out.println("[AuthFilter] Is comment owner: " + isOwner);
                return isOwner;
            }
        } catch (Exception e) {
            System.out.println("[AuthFilter] Ownership Check EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            JDBCUtil.close(conn, null);
        }

        System.out.println("[AuthFilter] Ownership Check END: No matching resource type, returning false");
        return false;
    }
    private static class PermissionCheckResult {
        String requiredPermission;
        boolean needOwnerCheck;

        PermissionCheckResult(String permission, boolean needOwnerCheck) {
            this.requiredPermission = permission;
            this.needOwnerCheck = needOwnerCheck;
        }
    }

    private PermissionCheckResult mapUriToPermission(String uri, String method, HttpServletRequest req) {
        System.out.println("[AuthFilter] DEBUG - URI: " + uri + ", Method: " + method);

        if (uri.startsWith("/api/video/")) {
            System.out.println("[AuthFilter] DEBUG - Matched video API");

            if (uri.contains("/upload") && "POST".equals(method)) {
                return new PermissionCheckResult("video:upload", false);
            } else if (uri.matches(".*/\\d+$") && "DELETE".equals(method)) {
                System.out.println("[AuthFilter] DEBUG - Matched video DELETE, need ownership check");
                return new PermissionCheckResult(null, true);
            } else if (uri.contains("/like") && "POST".equals(method)) {
                return new PermissionCheckResult("video:like", false);
            } else if (uri.contains("/unlike") && "POST".equals(method)) {
                return new PermissionCheckResult("video:like", false);
            } else if ("GET".equals(method)) {
                return new PermissionCheckResult("video:view", false);
            } else {
                System.out.println("[AuthFilter] DEBUG - Video API but no specific operation matched");
            }
        } else if (uri.startsWith("/api/comment/")) {
            System.out.println("[AuthFilter] DEBUG - Matched comment API");
            if ("POST".equals(method)) {
                return new PermissionCheckResult("comment:add", false);
            } else if (uri.matches(".*/\\d+$") && "DELETE".equals(method)) {
                return new PermissionCheckResult(null, true);
            }
        } else if (uri.startsWith("/api/follow/")) {
            if ("POST".equals(method)) {
                return new PermissionCheckResult("follow:manage", false);
            } else if ("GET".equals(method)) {
                return new PermissionCheckResult("follow:view", false);
            }
        } else {
            System.out.println("[AuthFilter] DEBUG - No known API prefix matched");
        }

        return new PermissionCheckResult(null, false);
    }

    @Override
    public void destroy() {
        System.out.println("AuthFilter 销毁");
    }
}

