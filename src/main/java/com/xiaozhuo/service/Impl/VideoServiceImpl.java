package com.xiaozhuo.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.VideoService;
import com.xiaozhuo.util.FileUtil;
import com.xiaozhuo.util.OSSUtil;
import com.xiaozhuo.util.RedisUtil;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoServiceImpl implements VideoService {

    private VideoDao videoDao = new VideoDaoImpl();

    private static final String VIDEO_CACHE_PREFIX = "video:";
    private static final int CACHE_EXPIRE_SECONDS = 3600;

    @Override
    public Map<String, Object> uploadVideo(VideoInfo video, byte[] videoBytes, String fileName, byte[] coverBytes, String coverFileName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String videoUrl = OSSUtil.uploadFile(videoBytes, fileName);
            video.setVideoUrl(videoUrl);
            video.setFileSize((long) videoBytes.length);
            video.setFormat(FileUtil.getFileExtension(fileName));

            if (coverBytes != null && coverBytes.length > 0) {
                String coverUrl = OSSUtil.uploadFile(coverBytes, coverFileName);
                video.setCover(coverUrl);
            }

            video.setCacheStatus(0);
            video.setTranscodeStatus(2);

            int rows = videoDao.insert(video);
            if (rows > 0) {
                clearVideoCache(video.getId());
                result.put("code", 200);
                result.put("message", "视频上传成功");
                result.put("data", video);
            } else {
                result.put("code", 500);
                result.put("message", "视频上传失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "上传异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getVideoById(Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            String cacheKey = VIDEO_CACHE_PREFIX + id;
            String cachedVideo = RedisUtil.get(cacheKey);

            VideoInfo video;
            if (cachedVideo != null) {
                video = JSON.parseObject(cachedVideo, VideoInfo.class);
                result.put("fromCache", true);
            } else {
                video = videoDao.selectById(id);
                if (video != null) {
                    RedisUtil.setex(cacheKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(video));
                    videoDao.incrementViewCount(id);
                    video.setViewCount(video.getViewCount() + 1);
                }
                result.put("fromCache", false);
            }

            if (video != null) {
                result.put("code", 200);
                result.put("message", "查询成功");
                result.put("data", video);
            } else {
                result.put("code", 404);
                result.put("message", "视频不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getVideoList(int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<VideoInfo> videos = videoDao.selectAll(pageNum, pageSize);
            int total = videoDao.countAll();

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", videos);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getVideosByAuthorId(Long authorId, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<VideoInfo> videos = videoDao.selectByAuthorId(authorId, pageNum, pageSize);
            int total = videoDao.countByAuthorId(authorId);

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", videos);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getVideosByCategoryId(Long categoryId, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<VideoInfo> videos = videoDao.selectByCategoryId(categoryId, pageNum, pageSize);
            int total = videoDao.countByCategoryId(categoryId);

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", videos);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> searchVideos(String keyword, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<VideoInfo> videos = videoDao.searchByTitle(keyword, pageNum, pageSize);

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", videos);
            result.put("total", videos.size());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "查询异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> deleteVideo(Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            VideoInfo video = videoDao.selectById(id);
            if (video == null) {
                result.put("code", 404);
                result.put("message", "视频不存在");
                return result;
            }

            int rows = videoDao.deleteById(id);
            if (rows > 0) {
                clearVideoCache(id);
                OSSUtil.deleteFile(video.getVideoUrl());
                if (video.getCover() != null && !video.getCover().contains("/default/")) {
                    OSSUtil.deleteFile(video.getCover());
                }

                result.put("code", 200);
                result.put("message", "删除成功");
            } else {
                result.put("code", 500);
                result.put("message", "删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "删除异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> likeVideo(Long userId, Long videoId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String likeKey = "like:user:" + userId + ":video:" + videoId;
            if (RedisUtil.exists(likeKey)) {
                result.put("code", 400);
                result.put("message", "已经点赞过");
                return result;
            }

            videoDao.incrementLikeCount(videoId);
            RedisUtil.setex(likeKey, 86400 * 30, "1");
            clearVideoCache(videoId);

            result.put("code", 200);
            result.put("message", "点赞成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "点赞异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> unlikeVideo(Long userId, Long videoId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String likeKey = "like:user:" + userId + ":video:" + videoId;
            if (!RedisUtil.exists(likeKey)) {
                result.put("code", 400);
                result.put("message", "还未点赞");
                return result;
            }

            videoDao.decrementLikeCount(videoId);
            RedisUtil.del(likeKey);
            clearVideoCache(videoId);

            result.put("code", 200);
            result.put("message", "取消点赞成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "取消点赞异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> cacheVideoToLocal(Long videoId) {
        Map<String, Object> result = new HashMap<>();
        try {
            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                result.put("code", 404);
                result.put("message", "视频不存在");
                return result;
            }

            result.put("code", 200);
            result.put("message", "缓存功能待实现，需要下载视频到本地");
            result.put("videoUrl", video.getVideoUrl());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "缓存异常：" + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getVideoDownloadUrl(Long videoId) {
        Map<String, Object> result = new HashMap<>();
        try {
            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                result.put("code", 404);
                result.put("message", "视频不存在");
                return result;
            }

            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", video);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", "获取异常：" + e.getMessage());
        }
        return result;
    }

    private void clearVideoCache(Long videoId) {
        RedisUtil.del(VIDEO_CACHE_PREFIX + videoId);
    }
}
