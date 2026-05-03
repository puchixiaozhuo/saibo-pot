package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.dao.VideoDao;
import com.xiaozhuo.dao.WatchHistoryDao;
import com.xiaozhuo.dao.impl.VideoDaoImpl;
import com.xiaozhuo.dao.impl.WatchHistoryDaoImpl;
import com.xiaozhuo.entity.UserWatchHistory;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.WatchHistoryService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Bean
public class WatchHistoryServiceImpl implements WatchHistoryService {

    private static final Logger logger = LogUtil.getLogger(WatchHistoryServiceImpl.class);

    private WatchHistoryDao watchHistoryDao = new WatchHistoryDaoImpl();
    private VideoDao videoDao = new VideoDaoImpl();




    @Override
    public Result<List<Map<String, Object>>> getWatchHistory(Long userId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            conn = ConnectionPool.getConnection();

            List<UserWatchHistory> histories = watchHistoryDao.selectByUserId(conn, userId, pageNum, pageSize);
            long total = watchHistoryDao.countByUserId(conn, userId);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (UserWatchHistory history : histories) {
                VideoInfo video = videoDao.selectById(history.getVideoId());

                Map<String, Object> item = new HashMap<>();
                item.put("historyId", history.getId());
                item.put("videoId", history.getVideoId());
                item.put("watchProgress", history.getWatchProgress());
                item.put("watchDuration", history.getWatchDuration());
                item.put("watchTime", history.getWatchTime());

                if (video != null) {
                    item.put("videoTitle", video.getTitle());
                    item.put("videoCover", video.getCover());
                    item.put("authorId", video.getAuthorId());
                    item.put("duration", video.getDuration());
                } else {
                    item.put("videoTitle", "视频已删除");
                    item.put("videoCover", null);
                    item.put("authorId", null);
                    item.put("duration", 0);
                }

                resultList.add(item);
            }

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("histories", resultList);
            pageInfo.put("total", total);
            pageInfo.put("pageNum", pageNum);
            pageInfo.put("pageSize", pageSize);
            pageInfo.put("totalPages", (total + pageSize - 1) / pageSize);

            return Result.success("查询成功", resultList);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询观看历史失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> reportWatchProgress(Long userId, Long videoId, Integer progress, Integer duration) {
        Connection conn = null;
        try {
            if (userId == null || videoId == null) {
                return Result.fail(400, "参数不能为空");
            }

            if (progress == null) progress = 0;
            if (duration == null) duration = 0;

            conn = ConnectionPool.getConnection();

            UserWatchHistory history = new UserWatchHistory();
            history.setUserId(userId);
            history.setVideoId(videoId);
            history.setWatchProgress(progress);
            history.setWatchDuration(duration);
            history.setWatchTime(LocalDateTime.now());

            watchHistoryDao.upsert(conn, history);

            System.out.println("✅ 观看进度上报成功 - UserID: " + userId
                    + ", VideoID: " + videoId
                    + ", Progress: " + progress + "s");

            return Result.success("观看进度上报成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "上报观看进度失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }



    @Override
    public Result<UserWatchHistory> getWatchRecord(Long userId, Long videoId) {
        Connection conn = null;
        try {
            if (userId == null || videoId == null) {
                return Result.fail(400, "参数不能为空");
            }

            conn = ConnectionPool.getConnection();

            UserWatchHistory history = watchHistoryDao.selectByUserIdAndVideoId(conn, userId, videoId);

            if (history == null) {
                return (Result<UserWatchHistory>) (Result<?>) Result.success("暂无观看记录", null);
            }

            return Result.success("查询成功", history);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询观看记录失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> deleteWatchRecord(Long userId, Long historyId) {
        Connection conn = null;
        try {
            if (userId == null || historyId == null) {
                return Result.fail(400, "参数不能为空");
            }

            conn = ConnectionPool.getConnection();

            UserWatchHistory history = watchHistoryDao.selectByUserIdAndVideoId(conn, userId, null);

            int rows = watchHistoryDao.deleteById(conn, historyId);
            if (rows <= 0) {
                return Result.fail(404, "观看记录不存在");
            }

            System.out.println("✅ 观看记录删除成功 - HistoryID: " + historyId);

            return Result.success("观看记录删除成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "删除观看记录失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> clearWatchHistory(Long userId) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            int rows = watchHistoryDao.deleteByUserId(conn, userId);

            System.out.println("✅ 观看历史清空成功 - UserID: " + userId
                    + ", Deleted " + rows + " records");

            return Result.success("观看历史清空成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "清空观看历史失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }
}