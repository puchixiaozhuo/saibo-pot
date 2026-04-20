package com.xiaozhuo.test;

import com.xiaozhuo.service.PermissionService;
import com.xiaozhuo.service.Impl.PermissionServiceImpl;
import com.xiaozhuo.util.JDBCUtil;
import com.xiaozhuo.util.PermissionCacheUtil;
import com.xiaozhuo.util.ResourcePermissionUtil;

import java.sql.Connection;
import java.util.Set;
import java.util.Map;

public class RBACDebugTest {

    public static void main(String[] args) {
        System.out.println("========== RBAC 权限系统调试 ==========\n");

        // 清除所有权限缓存
        System.out.println("【步骤 0】清除权限缓存");
        PermissionCacheUtil.clearAll();
        System.out.println("缓存已清除\n");

        PermissionService permissionService = new PermissionServiceImpl();
        Connection conn = null;

        try {
            conn = JDBCUtil.getConnection();

            // 测试两个用户
            Long userAId = 2L;  // 普通用户 A
            Long userBId = 3L;  // 普通用户 B
            Long adminId = 1L;  // 管理员

            System.out.println("【步骤 1】检查用户权限配置");
            System.out.println("-----------------------------------");
            checkUserPermissions(permissionService, userAId, "用户A");
            checkUserPermissions(permissionService, userBId, "用户B");
            checkUserPermissions(permissionService, adminId, "管理员");

            System.out.println("\n【步骤 2】检查视频所有权");
            System.out.println("-----------------------------------");
            testVideoOwnership(conn, userAId, userBId, adminId);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, null);
        }
    }

    private static void checkUserPermissions(PermissionService service, Long userId, String roleName) {
        System.out.println("\n" + roleName + " (userId=" + userId + "):");

        Map<String, Object> result = service.getUserPermissions(userId);
        if ((Integer) result.get("code") == 200) {
            Set<String> permissions = (Set<String>) result.get("data");
            System.out.println("  权限数量: " + permissions.size());

            boolean hasDeleteOwn = permissions.contains("video:delete:own");
            boolean hasDeleteAny = permissions.contains("video:delete:any");

            System.out.println("  - video:delete:own: " + hasDeleteOwn + (hasDeleteOwn ? " ✅" : " ❌"));
            System.out.println("  - video:delete:any: " + hasDeleteAny + (hasDeleteAny ? " ⚠️ 错误！" : " ✅"));

            // 直接调用 hasPermission 测试
            boolean canDeleteOwn = service.hasPermission(userId, "video:delete:own");
            boolean canDeleteAny = service.hasPermission(userId, "video:delete:any");

            System.out.println("  hasPermission 测试结果:");
            System.out.println("    - hasPermission(video:delete:own): " + canDeleteOwn);
            System.out.println("    - hasPermission(video:delete:any): " + canDeleteAny);
        } else {
            System.out.println("  查询失败: " + result.get("message"));
        }
    }

    private static void testVideoOwnership(Connection conn, Long userAId, Long userBId, Long adminId) {
        // 找一个测试视频
        String sql = "SELECT id, author_id FROM video_info WHERE is_delete = 0 LIMIT 1";
        java.sql.PreparedStatement pstmt = null;
        java.sql.ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Long videoId = rs.getLong("id");
                Long authorId = rs.getLong("author_id");

                System.out.println("\n测试视频 ID=" + videoId + ", 作者ID=" + authorId);

                boolean isUserAOwner = ResourcePermissionUtil.isVideoOwner(conn, userAId, videoId);
                boolean isUserBOwner = ResourcePermissionUtil.isVideoOwner(conn, userBId, videoId);
                boolean isAdminOwner = ResourcePermissionUtil.isVideoOwner(conn, adminId, videoId);

                System.out.println("  - 用户A (" + userAId + ") 是所有者: " + isUserAOwner +
                    (isUserAOwner ? " ✅" : ""));
                System.out.println("  - 用户B (" + userBId + ") 是所有者: " + isUserBOwner +
                    (isUserBOwner ? " ✅" : ""));
                System.out.println("  - 管理员 (" + adminId + ") 是所有者: " + isAdminOwner +
                    (isAdminOwner ? " ✅" : ""));

                System.out.println("\n权限判断逻辑:");
                PermissionService service = new PermissionServiceImpl();

                boolean userACanDelete = service.hasPermission(userAId, "video:delete:any") ||
                    (service.hasPermission(userAId, "video:delete:own") && isUserAOwner);
                boolean userBCanDelete = service.hasPermission(userBId, "video:delete:any") ||
                    (service.hasPermission(userBId, "video:delete:own") && isUserBOwner);

                System.out.println("  用户A 删除此视频: " + (userACanDelete ? "✅ 允许" : "❌ 禁止（不是自己的）"));
                System.out.println("  用户B 删除此视频: " + (userBCanDelete ? "✅ 允许" : "❌ 禁止（不是自己的）"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
