package com.xiaozhuo.service;

import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;

import java.util.List;
import java.util.Map;

public interface VideoService {
    /**
     * 上传视频
     * @param video 视频信息
     * @param videoBytes 视频文件字节数组
     * @param fileName 文件名
     * @param coverBytes 封面文件字节数组
     * @param coverFileName 封面文件名
     * @return 上传结果
     */
    Result<Map<String, Object>> uploadVideo(VideoInfo video, byte[] videoBytes, String fileName, byte[] coverBytes, String coverFileName);

    /**
     * 根据视频id获取视频信息
     * @param id 视频ID
     * @return 视频信息
     */
    Result<VideoInfo> getVideoById(Long id);

    /**
     * 获取视频列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> getVideoList(int pageNum, int pageSize);

    /**
     * 根据作者id获取视频列表
     * @param authorId 作者ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> getVideosByAuthorId(Long authorId, int pageNum, int pageSize);

    /**
     * 根据分类id获取视频列表
     * @param categoryId 分类ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> getVideosByCategoryId(Long categoryId, int pageNum, int pageSize);

    /**
     * 根据关键词搜索视频
     * @param keyword 搜索关键词
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    Result<List<VideoInfo>> searchVideos(String keyword, int pageNum, int pageSize);

    /**
     * 删除视频
     * @param id 视频ID
     * @return 删除结果
     */
    Result<Void> deleteVideo(Long id);

    /**
     * 点赞视频
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 点赞结果
     */
    Result<Void> likeVideo(Long userId, Long videoId);

    /**
     * 取消点赞视频
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 取消点赞结果
     */
    Result<Void> unlikeVideo(Long userId, Long videoId);

    /**
     * 收藏视频
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 收藏结果
     */
    Result<Void> favoriteVideo(Long userId, Long videoId);

    /**
     * 取消收藏视频
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 取消收藏结果
     */
    Result<Void> unfavoriteVideo(Long userId, Long videoId);

    /**
     * 获取用户收藏列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 收藏视频列表
     */
    Result<List<VideoInfo>> getUserFavorites(Long userId, int pageNum, int pageSize);


    /**
     * 将视频缓存到本地
     * @param videoId 视频ID
     * @return 缓存结果
     */
    Result<Map<String, Object>> cacheVideoToLocal(Long videoId);

    /**
     * 获取视频缓存状态
     * @param videoId 视频ID
     * @return 缓存状态信息
     */
    Result<Map<String, Object>> getCacheStatus(Long videoId);

    /**
     * 清理视频缓存
     * @param videoId 视频ID
     * @return 清理结果
     */
    Result<Void> clearCache(Long videoId);

    /**
     * 获取视频下载地址
     * @param videoId 视频ID
     * @return 视频信息
     */
    Result<VideoInfo> getVideoDownloadUrl(Long videoId);
}
