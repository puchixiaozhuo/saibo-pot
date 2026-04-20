package com.xiaozhuo.dao;

import com.xiaozhuo.entity.VideoInfo;

import java.sql.Connection;
import java.util.List;

public interface VideoDao {
    /**
     * 插入一条数据
     * @param video
     * @return
     */
    int insert(VideoInfo video);
    /**
     * 更新一条数据
     * @param video
     * @return
     */
    int update(VideoInfo video);
    /**
     * 根据id删除一条数据
     * @param id
     * @return
     */
    int deleteById(Long id);
    /**
     * 根据id查询一条数据
     * @param id
     * @return
     */
    VideoInfo selectById(Long id);
    /**
     * 查询所有视频
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<VideoInfo> selectAll(int pageNum, int pageSize);
    /**
     * 根据作者id查询视频
     * @param authorId
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<VideoInfo> selectByAuthorId(Long authorId, int pageNum, int pageSize);
    /**
     * 根据分类id查询视频
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<VideoInfo> selectByCategoryId(Long categoryId, int pageNum, int pageSize);
    /**
     * 根据标题查询视频
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<VideoInfo> searchByTitle(String keyword, int pageNum, int pageSize);
    /**
     * 查询所有视频数量
     * @return
     */
    int countAll();
    /**
     * 根据作者id查询视频数量
     * @param authorId
     * @return
     */
    int countByAuthorId(Long authorId);
    /**
     * 根据分类id查询视频数量
     * @param categoryId
     * @return
     */
    int countByCategoryId(Long categoryId);
    /**
     * 增加视频播放量
     * @param id
     * @return
     */
    int incrementViewCount(Long id);

    /**
     * 减少视频播放量
     * @param id
     * @return
     */
    int decrementViewCount(Long id);
    /**
     * 增加视频点赞量
     * @param id
     * @return
     */
    int incrementLikeCount(Long id);
    /**
     * 减少视频点赞量
     * @param id
     * @return
     */
    int decrementLikeCount(Long id);
    /**
     * 增加视频评论量
     * @param id
     * @return
     */
    int incrementCommentCount(Long id);
    /**
     * 减少视频评论量
     * @param id
     * @return
     */
    int decrementCommentCount(Long id);

    /**
     * 根据视频ID获取作者ID
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @return 作者ID
     */
    Long getAuthorIdById(Connection conn, Long videoId);
}


