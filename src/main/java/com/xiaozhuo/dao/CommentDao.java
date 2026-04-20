package com.xiaozhuo.dao;

import com.xiaozhuo.entity.VideoComment;
import java.sql.Connection;
import java.util.List;

public interface CommentDao {
    int insert(Connection conn, VideoComment comment);

    VideoComment selectById(Connection conn, Long id);

    List<VideoComment> selectByVideoIdOrderByHot(Connection conn, Long videoId, int pageNum, int pageSize);

    List<VideoComment> selectByVideoIdOrderByTime(Connection conn, Long videoId, int pageNum, int pageSize);

    int countByVideoId(Connection conn, Long videoId);

    int deleteById(Connection conn, Long id);

    int incrementLikeCount(Connection conn, Long commentId);

    int decrementLikeCount(Connection conn, Long commentId);

    List<VideoComment> selectByUserId(Connection conn, Long userId, int pageNum, int pageSize);

    /**
     * 根据评论ID获取作者ID
     * @param conn 数据库连接
     * @param commentId 评论ID
     * @return 作者ID
     */
    Long getAuthorIdById(Connection conn, Long commentId);
}
