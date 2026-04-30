package com.xiaozhuo.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.dao.FavoriteDao;
import com.xiaozhuo.dao.VideoCacheRecordDao;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.impl.FavoriteDaoImpl;
import com.xiaozhuo.dao.impl.VideoCacheRecordDaoImpl;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.entity.UserFavorite;
import com.xiaozhuo.entity.VideoCacheRecord;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.FeedService;
import com.xiaozhuo.service.VideoService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.FileUtil;
import com.xiaozhuo.util.OSSUtil;
import com.xiaozhuo.util.RedisUtil;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.sql.Connection;           // 数据库连接
import java.net.HttpURLConnection;    // HTTP 连接
import java.net.URL;                  // URL 类

/**
 * VideoService实现类 - 使用声明式事务和统一返回格式
 */
@Bean
public class VideoServiceImpl implements VideoService {

    private VideoDao videoDao = new VideoDaoImpl();
    private VideoCacheRecordDao cacheRecordDao = new VideoCacheRecordDaoImpl();
    private FavoriteDao favoriteDao = new FavoriteDaoImpl();
    private FeedService feedService = new FeedServiceImpl();


    private static final String VIDEO_CACHE_PREFIX = "video:";
    private static final int CACHE_EXPIRE_SECONDS = 3600;
    private static final int LOCAL_CACHE_TYPE = 1;
    private static final int CDN_CACHE_TYPE = 2;
    private static final int CACHE_EXPIRE_DAYS = 30;


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

            // 上传封面文件
            if (coverBytes != null && coverBytes.length > 0) {
                String coverUrl = OSSUtil.uploadFile(coverBytes, coverFileName);
                video.setCover(coverUrl);
            }

            // 设置默认值
            if (video.getViewCount() == null) video.setViewCount(0L);
            if (video.getLikeCount() == null) video.setLikeCount(0L);
            if (video.getCommentCount() == null) video.setCommentCount(0L);
            if (video.getFavoriteCount() == null) video.setFavoriteCount(0L);
            video.setCreateTime(java.time.LocalDateTime.now());
            video.setUpdateTime(java.time.LocalDateTime.now());

            // 插入数据库
            videoDao.insert(video);

            // 🔥 Push模式：将新视频推送到所有粉丝的Feed流
            try {
                feedService.pushFeedToFollowers(video.getAuthorId(), video.getId(), video.getCreateTime());
            } catch (Exception e) {
                System.err.println("⚠️ Push Feed 失败（不影响视频发布）: " + e.getMessage());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("videoId", video.getId());
            data.put("videoUrl", videoUrl);

            return Result.success("视频上传成功", data);
        } catch (Exception e) {
            e.printStackTrace();
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
    @Transactional
    public Result<Void> favoriteVideo(Long userId, Long videoId) {
        Connection conn = null;
        try {
            if (userId == null || videoId == null) {
                return Result.fail(400, "参数无效");
            }

            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                return Result.fail(404, "视频不存在");
            }

            conn = ConnectionPool.getConnection();

            UserFavorite existing = favoriteDao.selectByUserIdAndVideoId(conn, userId, videoId);
            if (existing != null) {
                return Result.fail(400, "已经收藏过");
            }

            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setVideoId(videoId);
            favorite.setCreateTime(LocalDateTime.now());

            favoriteDao.insert(conn, favorite);

            videoDao.incrementFavoriteCount(videoId);

            clearVideoCache(videoId);

            String userFavoriteKey = "favorite:user:" + userId + ":video:" + videoId;
            RedisUtil.setex(userFavoriteKey, 86400 * 30, "1");

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Void> unfavoriteVideo(Long userId, Long videoId) {
        Connection conn = null;
        try {
            if (userId == null || videoId == null) {
                return Result.fail(400, "参数无效");
            }

            conn = ConnectionPool.getConnection();

            UserFavorite existing = favoriteDao.selectByUserIdAndVideoId(conn, userId, videoId);
            if (existing == null) {
                return Result.fail(400, "还未收藏");
            }

            favoriteDao.deleteByUserIdAndVideoId(conn, userId, videoId);

            videoDao.decrementFavoriteCount(videoId);

            clearVideoCache(videoId);

            String userFavoriteKey = "favorite:user:" + userId + ":video:" + videoId;
            RedisUtil.del(userFavoriteKey);

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<VideoInfo>> getUserFavorites(Long userId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            conn = ConnectionPool.getConnection();

            List<UserFavorite> favorites = favoriteDao.selectByUserId(conn, userId, pageNum, pageSize);

            List<VideoInfo> videos = new ArrayList<>();
            for (UserFavorite favorite : favorites) {
                VideoInfo video = videoDao.selectById(favorite.getVideoId());
                if (video != null) {
                    videos.add(video);
                }
            }

            long total = favoriteDao.countByUserId(conn, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("videos", videos);
            data.put("total", total);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            data.put("totalPages", (total + pageSize - 1) / pageSize);

            return Result.success("查询成功", videos);
        } catch (Exception e) {
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> cacheVideoToLocal(Long videoId) {
        Connection conn = null;
        try {
            if (videoId == null || videoId <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            String cacheStatusKey = "video:cache:status:" + videoId;
            String cachedStatus = RedisUtil.get(cacheStatusKey);
            if (cachedStatus != null) {
                Map<String, Object> cachedData = JSON.parseObject(cachedStatus, Map.class);
                return Result.success("视频已缓存（Redis缓存）", cachedData);
            }

            conn = ConnectionPool.getConnection();

            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                return Result.fail(404, "视频不存在");
            }

            if (video.getVideoUrl() == null || video.getVideoUrl().isEmpty()) {
                return Result.fail(400, "视频地址不存在");
            }

            if (video.getCacheStatus() != null && video.getCacheStatus() == 1) {
                List<VideoCacheRecord> records = cacheRecordDao.selectByVideoId(conn, videoId);
                if (!records.isEmpty()) {
                    VideoCacheRecord record = records.get(0);
                    Map<String, Object> data = new HashMap<>();
                    data.put("videoId", videoId);
                    data.put("cachePath", record.getCachePath());
                    data.put("cacheSize", record.getCacheSize());
                    data.put("createTime", record.getCreateTime());

                    RedisUtil.setex(cacheStatusKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(data));

                    return Result.success("视频已缓存，无需重复操作", data);
                }
            }

            System.out.println("[Cache] Start downloading video from OSS: " + video.getVideoUrl());
            InputStream inputStream = downloadFromOSS(video.getVideoUrl());
            if (inputStream == null) {
                return Result.fail(500, "下载视频失败");
            }

            String fileName = videoId + "_" + video.getTitle() + "." + FileUtil.getFileExtension(video.getVideoUrl());
            String cachePath = FileUtil.saveToLocalCache(inputStream, fileName, video.getFileSize() != null ? video.getFileSize() : 0);
            inputStream.close();

            System.out.println("[Cache] Video saved to local: " + cachePath);

            java.io.File cacheFile = new java.io.File(cachePath);
            long actualFileSize = FileUtil.getFileSize(cacheFile);

            VideoCacheRecord cacheRecord = new VideoCacheRecord();
            cacheRecord.setVideoId(videoId);
            cacheRecord.setCacheType(LOCAL_CACHE_TYPE);
            cacheRecord.setCachePath(cachePath);
            cacheRecord.setCacheSize(actualFileSize);
            cacheRecord.setExpireTime(LocalDateTime.now().plusDays(CACHE_EXPIRE_DAYS));

            cacheRecordDao.insert(conn, cacheRecord);

            video.setCacheStatus(1);
            videoDao.update(video);

            clearVideoCache(videoId);

            Map<String, Object> data = new HashMap<>();
            data.put("videoId", videoId);
            data.put("cachePath", cachePath);
            data.put("cacheSize", actualFileSize);
            data.put("cacheStatus", 1);
            data.put("expireTime", cacheRecord.getExpireTime());

            RedisUtil.setex(cacheStatusKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(data));

            System.out.println("[Cache] Cache completed successfully for video: " + videoId);
            return Result.success("视频缓存成功", data);

        } catch (Exception e) {
            System.err.println("[Cache] Cache failed for video: " + videoId + ", error: " + e.getMessage());
            e.printStackTrace();
            return Result.fail(500, "视频缓存失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    public Result<Map<String, Object>> getCacheStatus(Long videoId) {
        Connection conn = null;
        try {
            if (videoId == null || videoId <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            String cacheStatusKey = "video:cache:status:" + videoId;
            String cachedStatus = RedisUtil.get(cacheStatusKey);
            if (cachedStatus != null) {
                Map<String, Object> cachedData = JSON.parseObject(cachedStatus, Map.class);
                return Result.success("查询成功（Redis缓存）", cachedData);
            }

            conn = ConnectionPool.getConnection();

            VideoInfo video = videoDao.selectById(videoId);
            if (video == null) {
                return Result.fail(404, "视频不存在");
            }

            List<VideoCacheRecord> records = cacheRecordDao.selectByVideoId(conn, videoId);

            Map<String, Object> data = new HashMap<>();
            data.put("videoId", videoId);
            data.put("cacheStatus", video.getCacheStatus());

            if (records != null && !records.isEmpty()) {
                data.put("cacheRecords", records);
                data.put("hasLocalCache", records.stream().anyMatch(r -> r.getCacheType() == LOCAL_CACHE_TYPE));
                data.put("hasCDNCache", records.stream().anyMatch(r -> r.getCacheType() == CDN_CACHE_TYPE));
            } else {
                data.put("hasLocalCache", false);
                data.put("hasCDNCache", false);
            }

            RedisUtil.setex(cacheStatusKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(data));

            return Result.success("查询成功", data);

        } catch (Exception e) {
            System.err.println("[Cache] Query cache status failed: " + e.getMessage());
            return Result.fail(500, "查询缓存状态失败");
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

        @Transactional
    public Result<Void> clearCache(Long videoId) {
        Connection conn = null;
        try {
            if (videoId == null || videoId <= 0) {
                return Result.fail(400, "视频ID无效");
            }

            String cacheStatusKey = "video:cache:status:" + videoId;
            RedisUtil.del(cacheStatusKey);

            conn = ConnectionPool.getConnection();

            List<VideoCacheRecord> records = cacheRecordDao.selectByVideoId(conn, videoId);

            if (records != null && !records.isEmpty()) {
                for (VideoCacheRecord record : records) {
                    if (record.getCacheType() == LOCAL_CACHE_TYPE && record.getCachePath() != null) {
                        boolean deleted = FileUtil.deleteLocalFile(record.getCachePath());
                        System.out.println("[Cache] Delete local file: " + record.getCachePath() + ", result: " + deleted);
                    }
                }

                cacheRecordDao.deleteByVideoId(conn, videoId);
            }

            VideoInfo video = videoDao.selectById(videoId);
            if (video != null) {
                video.setCacheStatus(0);
                videoDao.update(video);
            }

            clearVideoCache(videoId);

            System.out.println("[Cache] Cache cleared for video: " + videoId);
            return Result.success();

        } catch (Exception e) {
            System.err.println("[Cache] Clear cache failed: " + e.getMessage());
            return Result.fail(500, "缓存清理失败");
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }



    private InputStream downloadFromOSS(String ossUrl) {
        try {
            System.out.println("[Cache] Attempting to download from URL: " + ossUrl);

            if (ossUrl == null || ossUrl.trim().isEmpty()) {
                System.err.println("[Cache] ERROR: OSS URL is empty");
                return null;
            }

            URL url = new URL(ossUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            System.out.println("[Cache] Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                long contentLength = connection.getContentLengthLong();
                System.out.println("[Cache] Content length: " + contentLength + " bytes");
                return connection.getInputStream();
            } else {
                System.err.println("[Cache] Download failed, response code: " + responseCode);
                System.err.println("[Cache] Response message: " + connection.getResponseMessage());
                return null;
            }
        } catch (Exception e) {
            System.err.println("[Cache] Download exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
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
