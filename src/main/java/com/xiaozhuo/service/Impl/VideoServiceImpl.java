package com.xiaozhuo.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.VideoService;
import com.xiaozhuo.util.FileUtil;
import com.xiaozhuo.util.OSSUtil;
import com.xiaozhuo.util.RedisUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VideoService实现类 - 使用声明式事务和统一返回格式
 */
@Bean
public class VideoServiceImpl implements VideoService {

    private VideoDao videoDao = new VideoDaoImpl();

    private static final String VIDEO_CACHE_PREFIX = "video:";
    private static final int CACHE_EXPIRE_SECONDS = 3600;

    @Override
    @Transactional
    public Result<Map<String, Object>> uploadVideo(VideoInfo video, byte[] videoBytes, String fileName, byte[] coverBytes, String coverFileName) {
        try {
            // 参数校验
            if (video.getTitle() == null || video.getTitle().trim().isEmpty()) {
                return Result.fail(400, "视频标题不能为空");
            }
            if (video.getAuthorId() == null) {
                return Result.fail(400, "作者ID不能为空");
            }
            if (videoBytes == null || videoBytes.length == 0) {
                return Result.fail(400, "视频文件不能为空");
            }

            // 上传视频文件
            String videoUrl = OSSUtil.uploadFile(videoBytes, fileName);
            video.setVideoUrl(videoUrl);
            video.setFileSize((long) videoBytes.length);
            video.setFormat(FileUtil.getFileExtension(fileName));

            // 上传封面文件
            if (coverBytes != null && coverBytes.length > 0) {
                String coverUrl = OSSUtil.uploadFile(coverBytes, coverFileName);
                video.setCover(coverUrl);
            }

            // 设置默认值
            video.setCacheStatus(0);
            video.setTranscodeStatus(2);
            video.setViewCount(0L);
            video.setLikeCount(0L);
            video.setCommentCount(0L);

            // 保存到数据库
            int rows = videoDao.insert(video);
            if (rows > 0) {
                clearVideoCache(video.getId());
                Map<String, Object> data = new HashMap<>();
                data.put("videoId", video.getId());
                data.put("videoUrl", videoUrl);
                return Result.success("视频上传成功", data);
            } else {
                return Result.error();
            }
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    public Result<VideoInfo> getVideoById(Long id) {
        try {
            if (id == null || id <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            // 尝试从缓存获取
            String cacheKey = VIDEO_CACHE_PREFIX + id;
            String cachedVideo = RedisUtil.get(cacheKey);

            VideoInfo video;
            if (cachedVideo != null) {
                video = JSON.parseObject(cachedVideo, VideoInfo.class);
            } else {
                video = videoDao.selectById(id);
                if (video != null) {
                    RedisUtil.setex(cacheKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(video));
                    // 增加播放量
                    videoDao.incrementViewCount(id);
                    video.setViewCount(video.getViewCount() + 1);
                }
            }

            if (video != null) {
                return Result.success("查询成功", video);
            } else {
                return Result.fail(404, "视频不存在");
            }
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    public Result<List<VideoInfo>> getVideoList(int pageNum, int pageSize) {
        try {
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            List<VideoInfo> videos = videoDao.selectAll(pageNum, pageSize);
            int total = videoDao.countAll();

            Map<String, Object> data = new HashMap<>();
            data.put("videos", videos);
            data.put("total", total);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            data.put("totalPages", (total + pageSize - 1) / pageSize);

            return Result.success("查询成功", videos);
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    public Result<List<VideoInfo>> getVideosByAuthorId(Long authorId, int pageNum, int pageSize) {
        try {
            if (authorId == null || authorId <= 0) {
                return Result.fail(400, "作者ID无效");
            }

            List<VideoInfo> videos = videoDao.selectByAuthorId(authorId, pageNum, pageSize);
            return Result.success("查询成功", videos);
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    public Result<List<VideoInfo>> getVideosByCategoryId(Long categoryId, int pageNum, int pageSize) {
        try {
            if (categoryId == null || categoryId <= 0) {
                return Result.fail(400, "分类ID无效");
            }

            List<VideoInfo> videos = videoDao.selectByCategoryId(categoryId, pageNum, pageSize);
            return Result.success("查询成功", videos);
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    public Result<List<VideoInfo>> searchVideos(String keyword, int pageNum, int pageSize) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.fail(400, "搜索关键词不能为空");
            }

            List<VideoInfo> videos = videoDao.searchByTitle(keyword.trim(), pageNum, pageSize);
            return Result.success("搜索成功", videos);
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteVideo(Long id) {
        try {
            if (id == null || id <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            VideoInfo video = videoDao.selectById(id);
            if (video == null) {
                return Result.fail(404, "视频不存在");
            }

            int rows = videoDao.deleteById(id);
            if (rows > 0) {
                clearVideoCache(id);
                OSSUtil.deleteFile(video.getVideoUrl());
                if (video.getCover() != null && !video.getCover().contains("/default/")) {
                    OSSUtil.deleteFile(video.getCover());
                }
                return Result.success();
            } else {
                return Result.error();
            }
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    @Transactional
    public Result<Void> likeVideo(Long userId, Long videoId) {
        try {
            if (userId == null || videoId == null) {
                return Result.fail(400, "参数无效");
            }

            String likeKey = "like:user:" + userId + ":video:" + videoId;
            if (RedisUtil.exists(likeKey)) {
                return Result.fail(400, "已经点赞过");
            }

            videoDao.incrementLikeCount(videoId);
            RedisUtil.setex(likeKey, 86400 * 30, "1");
            clearVideoCache(videoId);

            return Result.success();
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    @Transactional
    public Result<Void> unlikeVideo(Long userId, Long videoId) {
        try {
            if (userId == null || videoId == null) {
                return Result.fail(400, "参数无效");
            }

            String likeKey = "like:user:" + userId + ":video:" + videoId;
            if (!RedisUtil.exists(likeKey)) {
                return Result.fail(400, "还未点赞");
            }

            videoDao.decrementLikeCount(videoId);
            RedisUtil.del(likeKey);
            clearVideoCache(videoId);

            return Result.success();
        } catch (Exception e) {
            return Result.error();
        }
    }

    @Override
    public Result<Map<String, Object>> cacheVideoToLocal(Long videoId) {
        try {
            if (videoId == null || videoId <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                return Result.fail(404, "视频不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("videoUrl", video.getVideoUrl());
            data.put("videoId", videoId);
            return Result.success("获取成功", data);
        } catch (Exception e) {
            return Result.error();
        }
    }



    @Override
    public Result<VideoInfo> getVideoDownloadUrl(Long videoId) {
        try {
            if (videoId == null || videoId <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                return Result.fail(404, "视频不存在");
            }

            return Result.success("获取成功", video);
        } catch (Exception e) {
            return Result.error();
        }
    }

    private void clearVideoCache(Long videoId) {
        RedisUtil.del(VIDEO_CACHE_PREFIX + videoId);
    }
}
