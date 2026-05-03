package com.xiaozhuo.dao;

import com.xiaozhuo.entity.VideoTag;
import com.xiaozhuo.entity.VideoTagRelation;

import java.sql.Connection;
import java.util.List;

public interface TagDao {
    /**
     * 插入标签
     * @param conn 数据库连接
     * @param tag 标签信息
     * @return 影响行数
     */
    int insert(Connection conn, VideoTag tag);

    /**
     * 更新标签
     * @param conn 数据库连接
     * @param tag 标签信息
     * @return 影响行数
     */
    int update(Connection conn, VideoTag tag);

    /**
     * 删除标签
     * @param conn 数据库连接
     * @param tagId 标签ID
     * @return 影响行数
     */
    int delete(Connection conn, Long tagId);

    /**
     * 根据ID查询标签
     * @param conn 数据库连接
     * @param tagId 标签ID
     * @return 标签信息
     */
    VideoTag selectById(Connection conn, Long tagId);

    /**
     * 根据标签名称查询
     * @param conn 数据库连接
     * @param tagName 标签名称
     * @return 标签信息
     */
    VideoTag selectByName(Connection conn, String tagName);

    /**
     * 查询所有标签（按使用次数降序）
     * @param conn 数据库连接
     * @return 标签列表
     */
    List<VideoTag> selectAll(Connection conn);

    /**
     * 增加标签使用次数
     * @param conn 数据库连接
     * @param tagId 标签ID
     * @return 影响行数
     */
    int incrementUseCount(Connection conn, Long tagId);

    /**
     * 减少标签使用次数
     * @param conn 数据库连接
     * @param tagId 标签ID
     * @return 影响行数
     */
    int decrementUseCount(Connection conn, Long tagId);

    /**
     * 插入视频-标签关联
     * @param conn 数据库连接
     * @param relation 关联信息
     * @return 影响行数
     */
    int insertRelation(Connection conn, VideoTagRelation relation);

    /**
     * 删除视频-标签关联
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @param tagId 标签ID
     * @return 影响行数
     */
    int deleteRelation(Connection conn, Long videoId, Long tagId);

    /**
     * 删除视频的所有标签关联
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @return 影响行数
     */
    int deleteRelationsByVideoId(Connection conn, Long videoId);

    /**
     * 查询视频的所有标签
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @return 标签列表
     */
    List<VideoTag> selectTagsByVideoId(Connection conn, Long videoId);

    /**
     * 根据标签ID查询关联的视频ID列表
     * @param conn 数据库连接
     * @param tagId 标签ID
     * @return 视频ID列表
     */
    List<Long> selectVideoIdsByTagId(Connection conn, Long tagId);
}