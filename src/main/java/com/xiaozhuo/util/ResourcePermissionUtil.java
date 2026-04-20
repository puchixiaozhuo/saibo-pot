package com.xiaozhuo.util;

import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.dao.impl.CommentDaoImpl;

import java.sql.Connection;

/**
 * 资源权限校验工具类
 * 用于校验用户是否对某个资源有操作权限
 */
public class ResourcePermissionUtil {

    private static VideoDao videoDao = new VideoDaoImpl();
    private static CommentDao commentDao = new CommentDaoImpl();

    /**
     * 检查用户是否是视频的作者
     */
    public static boolean isVideoOwner(Connection conn, Long userId, Long videoId) {
        try {
            Long authorId = videoDao.getAuthorIdById(conn, videoId);
            return authorId != null && authorId.equals(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查用户是否是评论的作者
     */
    public static boolean isCommentOwner(Connection conn, Long userId, Long commentId) {
        try {
            Long authorId = commentDao.getAuthorIdById(conn, commentId);
            return authorId != null && authorId.equals(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}