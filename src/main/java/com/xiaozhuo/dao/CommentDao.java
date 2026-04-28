package com.xiaozhuo.dao;

import com.xiaozhuo.entity.VideoComment;
import java.sql.Connection;
import java.util.List;

public interface CommentDao {
    /**
     * 插入一条评论
     * @param conn 数据库连接
     * @param comment 评论对象
     * @return 插入成功返回1，否则返回0
     */
    int insert(Connection conn, VideoComment comment);

    /**
     * 根据ID查询一条评论
     * @param conn 数据库连接
     * @param id 评论ID
     * @return 评论对象
     */
    VideoComment selectById(Connection conn, Long id);

    /**
     * 根据视频ID查询所有评论，按时间排序
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 评论列表
     */
    List<VideoComment> selectByVideoIdOrderByHot(Connection conn, Long videoId, int pageNum, int pageSize);

    /**
     * 根据视频ID查询所有评论，按时间排序
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 评论列表
     */
    List<VideoComment> selectByVideoIdOrderByTime(Connection conn, Long videoId, int pageNum, int pageSize);

    /**
     * 根据视频ID查询评论数量
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @return 评论数量
     */
    int countByVideoId(Connection conn, Long videoId);

    /**
     * 根据评论ID删除一条评论
     * @param conn 数据库连接
     * @param id 评论ID
     * @return 删除成功返回1，否则返回0
     */
    int deleteById(Connection conn, Long id);

    /**
     * 增加评论的点赞数
     * @param conn 数据库连接
     * @param commentId 评论ID
     * @return 增加成功返回1，否则返回0
     */
    int incrementLikeCount(Connection conn, Long commentId);

    /**
     * 减少评论的点赞数
     * @param conn 数据库连接
     * @param commentId 评论ID
     * @return 减少成功返回1，否则返回0
     */
    int decrementLikeCount(Connection conn, Long commentId);

    /**
     * 根据用户ID查询所有评论，按时间排序
     * @param conn 数据库连接
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 评论列表
     */
    List<VideoComment> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize);
    /**
     * 根据评论ID获取作者ID
     * @param conn 数据库连接
     * @param commentId 评论ID
     * @return 作者ID
     */
    Long getAuthorIdById(Connection conn, Long commentId);
}
