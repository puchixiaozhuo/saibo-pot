package com.xiaozhuo.test;

import com.xiaozhuo.util.JDBCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserRoleCheckTest {

    public static void main(String[] args) {
        System.out.println("========== 用户角色诊断 ==========\n");

        Connection conn = null;
        try {
            conn = JDBCUtil.getConnection();

            // 查看所有用户
            System.out.println("【所有用户列表】");
            String sql1 = "SELECT id, username, nickname, role, status FROM sys_user ORDER BY id";
            PreparedStatement pstmt1 = conn.prepareStatement(sql1);
            ResultSet rs1 = pstmt1.executeQuery();

            while (rs1.next()) {
                Long id = rs1.getLong("id");
                String username = rs1.getString("username");
                String nickname = rs1.getString("nickname");
                Integer role = rs1.getInt("role");
                Integer status = rs1.getInt("status");

                System.out.println(String.format("  ID=%d, username=%s, nickname=%s, role=%d, status=%d",
                        id, username, nickname, role, status));
            }

            System.out.println("\n【权限配置统计】");
            String sql2 = "SELECT role, COUNT(*) as cnt FROM sys_role_perm GROUP BY role";
            PreparedStatement pstmt2 = conn.prepareStatement(sql2);
            ResultSet rs2 = pstmt2.executeQuery();

            while (rs2.next()) {
                Integer role = rs2.getInt("role");
                Integer count = rs2.getInt("cnt");
                String roleName = role == 0 ? "普通用户" : (role == 1 ? "管理员" : "未知");
                System.out.println(String.format("  角色=%d (%s), 权限数量=%d", role, roleName, count));
            }

            System.out.println("\n【检查特定用户的角色】");
            Long[] testUserIds = {1L, 2L, 3L};
            for (Long userId : testUserIds) {
                String sql3 = "SELECT u.id, u.username, u.role FROM sys_user u WHERE u.id = ?";
                PreparedStatement pstmt3 = conn.prepareStatement(sql3);
                pstmt3.setLong(1, userId);
                ResultSet rs3 = pstmt3.executeQuery();

                if (rs3.next()) {
                    String username = rs3.getString("username");
                    Integer role = rs3.getInt("role");
                    String roleName = role == 0 ? "普通用户" : (role == 1 ? "管理员" : "未知");
                    System.out.println(String.format("  用户ID=%d, username=%s, role=%d (%s)",
                            userId, username, role, roleName));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, null);
        }
    }
}