package com.xiaozhuo.service;

import com.xiaozhuo.entity.VideoTag;
import com.xiaozhuo.result.Result;

import java.util.List;

public interface TagService {

    /**
     * 创建标签
     * @param tag 标签信息
     * @return 创建结果
     */
    Result<Long> createTag(VideoTag tag);

    /**
     * 更新标签
     * @param tag 标签信息
     * @return 更新结果
     */
    Result<Boolean> updateTag(VideoTag tag);

    /**
     * 删除标签
     * @param tagId 标签ID
     * @return 删除结果
     */
    Result<Boolean> deleteTag(Long tagId);

    /**
     * 查询所有标签
     * @return 标签列表
     */
    Result<List<VideoTag>> getAllTags();

    /**
     * 根据ID查询标签详情
     * @param tagId 标签ID
     * @return 标签信息
     */
    Result<VideoTag> getTagById(Long tagId);

    /**
     * 为视频添加标签
     * @param videoId 视频ID
     * @param tagIds 标签ID列表
     * @return 添加结果
     */
    Result<Boolean> addTagsToVideo(Long videoId, List<Long> tagIds);

    /**
     * 移除视频的标签
     * @param videoId 视频ID
     * @param tagId 标签ID
     * @return 移除结果
     */
    Result<Boolean> removeTagFromVideo(Long videoId, Long tagId);

    /**
     * 查询视频的标签列表
     * @param videoId 视频ID
     * @return 标签列表
     */
    Result<List<VideoTag>> getTagsByVideoId(Long videoId);

    /**
     * 根据标签查询视频列表
     * @param tagId 标签ID
     * @return 视频ID列表
     */
    Result<List<Long>> getVideosByTagId(Long tagId);
}