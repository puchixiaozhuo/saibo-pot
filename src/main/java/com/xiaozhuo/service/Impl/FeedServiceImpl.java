package com.xiaozhuo.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.dao.FollowDao;
import com.xiaozhuo.dao.UserDao;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.impl.FollowDaoImpl;
import com.xiaozhuo.dao.impl.UserDaoImpl;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.FeedService;
import com.xiaozhuo.util.AsyncExecutor;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.RedisUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Feed流服务实现类 - Pull模式
 */
@Bean
public class FeedServiceImpl implements FeedService {

    private FollowDao followDao = new FollowDaoImpl();
    private VideoDao videoDao = new VideoDaoImpl();
    private UserDao userDao = new UserDaoImpl();

    private static final String FEED_CACHE_PREFIX = "feed:user:";
    private static final int FEED_CACHE_EXPIRE_SECONDS = 300; // 5分钟缓存

    private static final String FEED_PUSH_PREFIX = "feed:push:";
    private static final int FEED_PUSH_MAX_SIZE = 1000; // 每个用户最多保留1000条Feed
    private static final int FEED_PUSH_EXPIRE_DAYS = 7; // Feed保留7天

    @Override
    public Result<List<VideoInfo>> pullFeed(Long userId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            String cacheKey = FEED_CACHE_PREFIX + userId + ":page:" + pageNum + ":size:" + pageSize;
            String cachedFeed = RedisUtil.get(cacheKey);

            if (cachedFeed != null) {
                List<VideoInfo> videos = JSON.parseArray(cachedFeed, VideoInfo.class);
                return Result.success("查询成功（缓存）", videos);
            }

            conn = ConnectionPool.getConnection();

            List<Long> followingIds = followDao.getFollowingUserIds(conn, userId);

            if (followingIds == null || followingIds.isEmpty()) {
                return Result.success("暂无关注用户", new ArrayList<>());
            }

            List<VideoInfo> videos = videoDao.selectVideosByAuthorIds(followingIds, pageNum, pageSize);

            long total = videoDao.countVideosByAuthorIds(followingIds);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("videos", videos);
            pageInfo.put("total", total);
            pageInfo.put("pageNum", pageNum);
            pageInfo.put("pageSize", pageSize);
            pageInfo.put("totalPages", (total + pageSize - 1) / pageSize);

            RedisUtil.setex(cacheKey, FEED_CACHE_EXPIRE_SECONDS, JSON.toJSONString(videos));

            return Result.success("查询成功", videos);

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
    public Result<List<VideoInfo>> pullFeedWithCursor(Long userId, String cursor, int pageSize) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            conn = ConnectionPool.getConnection();

            List<Long> followingIds = followDao.getFollowingUserIds(conn, userId);

            if (followingIds == null || followingIds.isEmpty()) {
                return Result.success("暂无关注用户", new ArrayList<>());
            }

            List<VideoInfo> videos;
            if (cursor == null || cursor.isEmpty()) {
                videos = videoDao.selectVideosByAuthorIdsWithCursor(followingIds, null, pageSize);
            } else {
                videos = videoDao.selectVideosByAuthorIdsWithCursor(followingIds, cursor, pageSize);
            }

            String nextCursor = null;
            if (!videos.isEmpty()) {
                nextCursor = String.valueOf(videos.get(videos.size() - 1).getId());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("videos", videos);
            data.put("nextCursor", nextCursor);
            data.put("hasMore", !videos.isEmpty() && videos.size() == pageSize);

            return Result.success("查询成功", videos);

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
    public Result<Integer> pushFeedToFollowers(Long authorId, Long videoId, java.time.LocalDateTime publishTime) {
        Connection conn = null;
        try {
            if (authorId == null || videoId == null) {
                return Result.fail(400, "Invalid parameters");
            }

            conn = ConnectionPool.getConnection();

            List<Long> followerIds = followDao.getFollowerUserIds(conn, authorId);

            if (followerIds == null || followerIds.isEmpty()) {
                return Result.success("This author has no followers", 0);
            }

            long score = publishTime != null ? publishTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis();

            // 异步推送到每个粉丝
            int totalFans = followerIds.size();
            for (Long followerId : followerIds) {
                AsyncExecutor.submit(() -> {
                    try {
                        String feedKey = FEED_PUSH_PREFIX + followerId;
                        RedisUtil.zadd(feedKey, score, String.valueOf(videoId));
                        RedisUtil.zremrangeByRank(feedKey, 0, -(FEED_PUSH_MAX_SIZE + 1));
                        RedisUtil.expire(feedKey, FEED_PUSH_EXPIRE_DAYS * 86400);
                    } catch (Exception e) {
                        System.err.println(" Push to follower " + followerId + " failed: " + e.getMessage());
                    }
                });
            }

            System.out.println(" Push Feed initiated: AuthorID=" + authorId + ", VideoID=" + videoId
                    + ", Total fans=" + totalFans + " (async)");

            return Result.success("Push initiated (async)", totalFans);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * 计算视频的热度分数（牛顿冷却定律）
     */
    private double calculateHotnessScore(VideoInfo video) {
        long now = System.currentTimeMillis();
        long createTime = video.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        // T: 距离现在的小时数
        double hoursPassed = (now - createTime) / (1000.0 * 3600);

        // P: 互动得分 (播放*1 + 点赞*5 + 评论*10)
        double points = (video.getViewCount() != null ? video.getViewCount() : 0) * 1.0
                + (video.getLikeCount() != null ? video.getLikeCount() : 0) * 5.0
                + (video.getCommentCount() != null ? video.getCommentCount() : 0) * 10.0;

        // G: 重力因子，越大衰减越快
        double gravity = 1.8;

        // 避免分母为 0
        return points / Math.pow(hoursPassed + 2, gravity);
    }


    @Override
    public Result<Object> pullPushFeed(Long userId, int pageNum, int pageSize) {
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            String feedKey = FEED_PUSH_PREFIX + userId;

            long start = (pageNum - 1) * pageSize;
            long end = start + pageSize - 1;

            List<String> videoIds = RedisUtil.zrevrange(feedKey, start, end);

            if (videoIds == null || videoIds.isEmpty()) {
                Map<String, Object> emptyData = new HashMap<>();
                emptyData.put("videos", new ArrayList<>());
                emptyData.put("serverTime", LocalDateTime.now().toString());
                // 🔥 修复：通过中间变量进行泛型转换
                Result<Object> result = Result.success("暂无Feed数据", (Object) emptyData);
                return result;
            }

            List<Long> videoIdList = new ArrayList<>();
            for (String videoIdStr : videoIds) {
                videoIdList.add(Long.parseLong(videoIdStr));
            }

            List<VideoInfo> videos = videoDao.selectVideosByIds(videoIdList);

            // 🔥 高级货：根据热度重新排序
            if (videos != null && !videos.isEmpty()) {
                videos.sort((v1, v2) -> Double.compare(calculateHotnessScore(v2), calculateHotnessScore(v1)));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("videos", videos);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            data.put("serverTime", LocalDateTime.now().toString());

            // 🔥 修复：通过中间变量进行泛型转换
            Result<Object> result = Result.success("Query successful", (Object) data);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        }
    }



    @Override
    public Result<Long> getUnreadFeedCount(Long userId, java.time.LocalDateTime lastReadTime) {
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            String feedKey = FEED_PUSH_PREFIX + userId;

            long lastReadTimestamp = lastReadTime != null
                    ? lastReadTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : 0;

            Long count = RedisUtil.zcount(feedKey, lastReadTimestamp + 1, Double.MAX_VALUE);

            return Result.success("查询成功", count != null ? count : 0L);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        }
    }

    @Override
    public Result<Long> getUnreadFeedCountFromDB(Long userId, java.time.LocalDateTime lastReadTime) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (lastReadTime == null) {
                return Result.fail(400, "最后阅读时间不能为空");
            }

            conn = ConnectionPool.getConnection();

            List<Long> followingIds = followDao.getFollowingUserIds(conn, userId);

            if (followingIds == null || followingIds.isEmpty()) {
                return Result.success("暂无关注用户", 0L);
            }

            long unreadCount = videoDao.countUnreadVideos(followingIds, lastReadTime);

            return Result.success("查询成功", unreadCount);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

}