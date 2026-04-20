package com.xiaozhuo.service;

import com.xiaozhuo.entity.VideoInfo;
import java.util.List;
import java.util.Map;

public interface VideoService {
    /**
     * 上传视频
     * @param video
     * @param videoBytes
     * @param fileName
     * @param coverBytes
     * @param coverFileName
     * @return
     */
    Map<String, Object> uploadVideo(VideoInfo video, byte[] videoBytes, String fileName, byte[] coverBytes, String coverFileName);
    /**
     * 根据视频id获取视频信息
     * @param id
     * @return
     */
    Map<String, Object> getVideoById(Long id);
    /**
     * 获取视频列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    Map<String, Object> getVideoList(int pageNum, int pageSize);
    /**
     * 根据作者id获取视频列表
     * @param authorId
     * @param pageNum
     * @param pageSize
     * @return
     */
    Map<String, Object> getVideosByAuthorId(Long authorId, int pageNum, int pageSize);
    /**
     * 根据分类id获取视频列表
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @return
     */
    Map<String, Object> getVideosByCategoryId(Long categoryId, int pageNum, int pageSize);
    /**
     * 根据关键词搜索视频
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @return
     */
    Map<String, Object> searchVideos(String keyword, int pageNum, int pageSize);
    /**
     * 删除视频
     * @param id
     * @return
     */
    Map<String, Object> deleteVideo(Long id);
    /**
     * 点赞视频
     * @param userId
     * @param videoId
     * @return
     */
    Map<String, Object> likeVideo(Long userId, Long videoId);
    /**
     * 取消点赞视频
     * @param userId
     * @param videoId
     * @return
     */
    Map<String, Object> unlikeVideo(Long userId, Long videoId);
    /**
     * 将视频缓存到本地
     * @param videoId
     * @return
     */
    Map<String, Object> cacheVideoToLocal(Long videoId);
    /**
     * 获取视频下载地址
     * @param videoId
     * @return
     */
    Map<String, Object> getVideoDownloadUrl(Long videoId);
}