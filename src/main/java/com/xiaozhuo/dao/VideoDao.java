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
     * 增加视频收藏量
     * @param id
     * @return
     */
    int incrementFavoriteCount(Long id);

    /**
     * 减少视频收藏量
     * @param id
     * @return
     */
    int decrementFavoriteCount(Long id);

    /**
     * 根据视频ID获取作者ID
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @return 作者ID
     */
    Long getAuthorIdById(Connection conn, Long videoId);

    /**
     * 根据多个作者ID查询视频列表（分页）
     * @param authorIds 作者ID列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    List<VideoInfo> selectVideosByAuthorIds(List<Long> authorIds, int pageNum, int pageSize);

    /**
     * 统计多个作者的視頻总数
     * @param authorIds 作者ID列表
     * @return 视频总数
     */
    long countVideosByAuthorIds(List<Long> authorIds);

    /**
     * 基于游标查询视频（加分项）
     * @param authorIds 作者ID列表
     * @param cursor 游标（最后一条视频ID）
     * @param pageSize 每页大小
     * @return 视频列表
     */
    List<VideoInfo> selectVideosByAuthorIdsWithCursor(List<Long> authorIds, String cursor, int pageSize);

    /**
     * 统计用户的未读Feed数量
     * @param authorIds 关注的作者ID列表
     * @param lastReadTime 最后读取时间
     * @return 未读视频数量
     */
    long countUnreadVideos(List<Long> authorIds, java.time.LocalDateTime lastReadTime);

    /**
     * 根据多个视频ID查询视频列表（用于Push模式）
     * @param videoIds 视频ID列表
     * @return 视频列表
     */
    List<VideoInfo> selectVideosByIds(List<Long> videoIds);
}

