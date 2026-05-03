package com.xiaozhuo.service.Impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.bean.dto.CouponActivityDTO;
import com.xiaozhuo.constant.MQConstant;
import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.FavoriteDao;
import com.xiaozhuo.dao.VideoCacheRecordDao;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.dao.impl.FavoriteDaoImpl;
import com.xiaozhuo.dao.impl.VideoCacheRecordDaoImpl;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.entity.*;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.FeedService;
import com.xiaozhuo.service.VideoService;
import com.xiaozhuo.util.*;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final java.util.logging.Logger logger = LogUtil.getLogger(VideoServiceImpl.class);


    private VideoDao videoDao = new VideoDaoImpl();
    private VideoCacheRecordDao cacheRecordDao = new VideoCacheRecordDaoImpl();
    private FavoriteDao favoriteDao = new FavoriteDaoImpl();
    private CouponDao couponDao = new CouponDaoImpl();
    private FeedService feedService = new FeedServiceImpl();


    private static final String VIDEO_CACHE_PREFIX = "video:";
    private static final int CACHE_EXPIRE_SECONDS = 3600;
    private static final int LOCAL_CACHE_TYPE = 1;
    private static final int CDN_CACHE_TYPE = 2;
    private static final int CACHE_EXPIRE_DAYS = 30;


    @Override
    @Transactional
    public Result<Map<String, Object>> uploadVideo(VideoInfo video, byte[] videoBytes, String fileName, byte[] coverBytes, String coverFileName, String couponActivityJson) {
        Entry entry = null;
        Connection conn = null;
        try {
            // 🔥 Sentinel 限流检查
            entry = SphU.entry("video:upload");

            if (video.getTitle() == null || video.getTitle().trim().isEmpty()) {
                return Result.fail(400, "视频标题不能为空");
            }
            if (video.getAuthorId() == null) {
                return Result.fail(400, "作者ID不能为空");
            }
            if (videoBytes == null || videoBytes.length == 0) {
                return Result.fail(400, "视频文件不能为空");
            }

            String videoUrl = OSSUtil.uploadFile(videoBytes, fileName);
            video.setVideoUrl(videoUrl);
            video.setFileSize((long) videoBytes.length);

            if (coverBytes != null && coverBytes.length > 0) {
                String coverUrl = OSSUtil.uploadFile(coverBytes, coverFileName);
                video.setCover(coverUrl);
            }

            if (video.getViewCount() == null) video.setViewCount(0L);
            if (video.getLikeCount() == null) video.setLikeCount(0L);
            if (video.getCommentCount() == null) video.setCommentCount(0L);
            if (video.getFavoriteCount() == null) video.setFavoriteCount(0L);
            video.setCreateTime(LocalDateTime.now());
            video.setUpdateTime(LocalDateTime.now());

            conn = TransactionManager.getConnection();
            videoDao.insert(video);

            Long videoId = video.getId();

            // 🔥 垂直分表：将 description 单独存入扩展表
            if (video.getDescription() != null && !video.getDescription().trim().isEmpty()) {
                saveVideoDescription(conn, videoId, video.getDescription());
            }

            if (couponActivityJson != null && !couponActivityJson.trim().isEmpty()) {
                try {
                    CouponActivityDTO activityDTO = JSON.parseObject(couponActivityJson, CouponActivityDTO.class);

                    String validationError = validateCouponActivity(activityDTO, videoId);
                    if (validationError != null) {
                        throw new BusinessException(400, validationError);
                    }

                    CouponActivity activity = new CouponActivity();
                    activity.setVideoId(videoId);
                    activity.setActivityType(activityDTO.getActivityType());
                    activity.setTitle(activityDTO.getTitle());
                    activity.setDescription(activityDTO.getDescription());
                    activity.setDiscountContent(activityDTO.getDiscountContent());
                    activity.setTotalStock(activityDTO.getTotalStock());
                    activity.setRemainingStock(activityDTO.getTotalStock());
                    activity.setStartTime(LocalDateTime.parse(activityDTO.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    activity.setEndTime(LocalDateTime.parse(activityDTO.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    activity.setStatus(0);
                    activity.setVersion(0);
                    activity.setRequiredWatchSeconds(activityDTO.getRequiredWatchSeconds() != null ? activityDTO.getRequiredWatchSeconds() : 0);

                    if (activityDTO.getBatches() != null && !activityDTO.getBatches().isEmpty()) {
                        activity.setBatchConfig(JSON.toJSONString(activityDTO.getBatches()));
                    }

                    if (activityDTO.getLotteryConfig() != null) {
                        activity.setLotteryConfig(JSON.toJSONString(activityDTO.getLotteryConfig()));
                    }

                    couponDao.insertActivity(conn, activity);

                    Long activityId = activity.getId();

                    if (activityDTO.getActivityType() == 2 && activityDTO.getBatches() != null) {
                        for (CouponActivityDTO.BatchConfigDTO batchDTO : activityDTO.getBatches()) {
                            CouponBatch batch = new CouponBatch();
                            batch.setActivityId(activityId);
                            batch.setBatchNumber(batchDTO.getBatchNumber());
                            batch.setStockCount(batchDTO.getStockCount());
                            batch.setReleasedStock(0);
                            batch.setReleaseTime(LocalDateTime.parse(batchDTO.getReleaseTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            batch.setStatus(0);
                            couponDao.insertBatch(conn, batch);
                        }
                    }

                    String stockKey = "coupon:stock:" + activityId;
                    RedisUtil.set(stockKey, String.valueOf(activity.getTotalStock()));

                    LogUtil.logBusiness(logger, "CREATE_COUPON_ACTIVITY",
                            "Video " + videoId + " created coupon activity " + activityId + ", type: " + activityDTO.getActivityType());

                } catch (BusinessException e) {
                    throw e;
                } catch (Exception e) {
                    LogUtil.logError(logger, "创建优惠券活动失败", e);
                    throw new BusinessException(500, "创建优惠券活动失败：" + e.getMessage());
                }
            }

            // 🔥 使用 RocketMQ 异步推送 Feed 流
            try {
                Map<String, Object> feedMessage = new HashMap<>();
                feedMessage.put("authorId", video.getAuthorId());
                feedMessage.put("videoId", video.getId());
                feedMessage.put("publishTime", video.getCreateTime().toString());

                String messageBody = JSON.toJSONString(feedMessage);
                RocketMQUtil.sendAsyncMessage(
                        MQConstant.TOPIC_FEED_PUSH,
                        MQConstant.TAG_FEED_NEW_VIDEO,
                        messageBody
                );

                LogUtil.logBusiness(logger, "FEED_PUSH_MESSAGE_SENT",
                        "Video " + videoId + " feed push message sent to MQ");
            } catch (Exception e) {
                System.err.println("⚠️ Feed Push MQ 发送失败（不影响视频发布）: " + e.getMessage());
                e.printStackTrace();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("videoId", video.getId());
            data.put("videoUrl", videoUrl);

            return Result.success("视频上传成功", data);
        } catch (BlockException ex) {
            // 🔥 触发限流
            logger.warning("Video upload blocked by Sentinel: " + ex.getClass().getSimpleName());
            return Result.fail(429, "系统繁忙，视频上传过于频繁，请稍后重试");
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            // 🔥 确保退出 Sentinel 上下文
            if (entry != null) {
                entry.exit();
            }
            if (conn != null && !TransactionManager.hasActiveTransaction()) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<VideoInfo> getVideoById(Long id, Long viewerId) {
        try {
            if (id == null || id <= 0) {
                return Result.fail(400, "Invalid video ID");
            }

            // 1. 尝试从缓存获取
            String cacheKey = VIDEO_CACHE_PREFIX + id;
            String cachedVideo = RedisUtil.get(cacheKey);

            VideoInfo video;
            if (cachedVideo != null) {
                video = JSON.parseObject(cachedVideo, VideoInfo.class);
            } else {
                // 2. 缓存没有，查数据库（主表）
                video = videoDao.selectById(id);
                if (video != null) {
                    // 🔥 3. 垂直分表：单独查询 description 大字段
                    String description = getVideoDescription(id);
                    video.setDescription(description);

                    // 4. 写入缓存（TTL 30 分钟）
                    RedisUtil.setex(cacheKey, CACHE_EXPIRE_SECONDS, JSON.toJSONString(video));
                }
            }

            if (video != null) {
                // 5. 防刷逻辑：只有有效观看才增加数据库计数
                if (viewerId != null && ViewCounterUtil.recordValidView(viewerId, id)) {
                    videoDao.incrementViewCount(id);
                    System.out.println(" Valid view recorded for user: " + viewerId + ", video: " + id);
                }

                return Result.success("Query successful", video);
            } else {
                return Result.fail(404, "Video not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        }
    }

    /**
     * 保存视频描述到垂直分表
     * @param conn 数据库连接
     * @param videoId 视频ID
     * @param description 视频描述
     */
    private void saveVideoDescription(Connection conn, Long videoId, String description) {
        String sql = "INSERT INTO video_info_detail (video_id, description) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE description = VALUES(description)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, videoId);
            ps.setString(2, description);
            ps.executeUpdate();

            LogUtil.logBusiness(logger, "VIDEO_DESCRIPTION_SAVED",
                    "Video " + videoId + " description saved to sharding table");
        } catch (SQLException e) {
            LogUtil.logError(logger, "保存视频描述失败: videoId=" + videoId, e);
            // 不抛出异常，description 保存失败不影响主流程
        }
    }

    /**
     * 从垂直分表中获取视频描述
     * @param videoId 视频ID
     * @return 视频描述，如果没有则返回 null
     */
    private String getVideoDescription(Long videoId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            String sql = "SELECT description FROM video_info_detail WHERE video_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, videoId);
            ResultSet rs = ps.executeQuery();

            String description = null;
            if (rs.next()) {
                description = rs.getString("description");
            }

            rs.close();
            ps.close();
            return description;

        } catch (SQLException e) {
            LogUtil.logError(logger, "查询视频描述失败: videoId=" + videoId, e);
            return null;
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
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

    private String validateCouponActivity(CouponActivityDTO dto, Long videoId) {
        if (dto.getActivityType() == null) {
            return "活动类型不能为空";
        }

        if (dto.getActivityType() == 0) {
            return null;
        }

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return "活动标题不能为空";
        }

        if (dto.getDiscountContent() == null || dto.getDiscountContent().trim().isEmpty()) {
            return "优惠内容不能为空";
        }

        if (dto.getTotalStock() == null || dto.getTotalStock() <= 0) {
            return "总库存必须大于0";
        }

        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            return "开始时间和结束时间不能为空";
        }

        LocalDateTime startTime = LocalDateTime.parse(dto.getStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse(dto.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now)) {
            return "开始时间不能早于当前时间";
        }

        if (startTime.isAfter(endTime)) {
            return "开始时间不能晚于结束时间";
        }

        switch (dto.getActivityType()) {
            case 1:
                return null;

            case 2:
                if (dto.getBatches() == null || dto.getBatches().size() < 2) {
                    return "分批次模式至少需要配置2个批次";
                }

                int totalBatchStock = dto.getBatches().stream()
                        .mapToInt(CouponActivityDTO.BatchConfigDTO::getStockCount)
                        .sum();

                if (totalBatchStock != dto.getTotalStock()) {
                    return "各批次库存之和必须等于总库存";
                }

                LocalDateTime lastReleaseTime = null;
                for (CouponActivityDTO.BatchConfigDTO batch : dto.getBatches()) {
                    LocalDateTime releaseTime = LocalDateTime.parse(batch.getReleaseTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    if (releaseTime.isBefore(startTime) || releaseTime.isAfter(endTime)) {
                        return "批次释放时间必须在活动时间内";
                    }

                    if (lastReleaseTime != null && !releaseTime.isAfter(lastReleaseTime)) {
                        return "批次释放时间必须递增";
                    }

                    lastReleaseTime = releaseTime;
                }
                return null;

            case 3:
                if (dto.getLotteryConfig() == null) {
                    return "抽签模式必须配置抽签信息";
                }

                LocalDateTime lotteryTime = LocalDateTime.parse(
                        dto.getLotteryConfig().getLotteryTime(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );

                if (!lotteryTime.isAfter(endTime)) {
                    return "开奖时间必须在活动结束后";
                }

                if (dto.getLotteryConfig().getWinnerCount() == null || dto.getLotteryConfig().getWinnerCount() <= 0) {
                    return "中签人数必须大于0";
                }

                if (dto.getLotteryConfig().getWinnerCount() > dto.getTotalStock()) {
                    return "中签人数不能超过总库存";
                }
                return null;

            case 4:
                if (dto.getRequiredWatchSeconds() == null || dto.getRequiredWatchSeconds() < 10) {
                    return "观看解锁模式需设置观看时长（至少10秒）";
                }
                return null;

            default:
                return "无效的活动类型";
        }
    }
}
