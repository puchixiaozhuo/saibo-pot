package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.VideoCacheRecordDao;
import com.xiaozhuo.entity.VideoCacheRecord;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VideoCacheRecordDaoImpl implements VideoCacheRecordDao {

    private static final Logger logger = LogUtil.getLogger(VideoCacheRecordDaoImpl.class);

    /**
     * 插入缓存记录
     *
     * @param conn
     * @param record
     * @return
     */
    @Override
    public int insert(Connection conn, VideoCacheRecord record) {
        String sql = "INSERT INTO video_cache_record (video_id, cache_type, cache_path, cache_url, cache_size, expire_time, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, record.getVideoId());
            ps.setInt(2, record.getCacheType());
            ps.setString(3, record.getCachePath());
            ps.setString(4, record.getCacheUrl());
            ps.setLong(5, record.getCacheSize());
            ps.setTimestamp(6, record.getExpireTime() != null ? Timestamp.valueOf(record.getExpireTime()) : null);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    record.setId(rs.getLong(1));
                }
            }
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "插入缓存记录失败", e);
            throw new DatabaseException("插入缓存记录失败", e);
        }
    }

    /**
     * 更新缓存记录
     *
     * @param conn
     * @param record
     * @return
     */
    @Override
    public int update(Connection conn, VideoCacheRecord record) {
        String sql = "UPDATE video_cache_record SET cache_type=?, cache_path=?, cache_url=?, cache_size=?, expire_time=?, update_time=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getCacheType());
            ps.setString(2, record.getCachePath());
            ps.setString(3, record.getCacheUrl());
            ps.setLong(4, record.getCacheSize());
            ps.setTimestamp(5, record.getExpireTime() != null ? Timestamp.valueOf(record.getExpireTime()) : null);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(7, record.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "更新缓存记录失败: id=" + record.getId(), e);
            throw new DatabaseException("更新缓存记录失败", e);
        }
    }

    /**
     * 删除缓存记录
     *
     * @param conn
     * @param id
     * @return
     */
    @Override
    public int deleteById(Connection conn, Long id) {
        String sql = "DELETE FROM video_cache_record WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "删除缓存记录失败: id=" + id, e);
            throw new DatabaseException("删除缓存记录失败", e);
        }
    }

    /**
     * 查询缓存记录
     *
     * @param conn
     * @param id
     * @return
     */
    @Override
    public VideoCacheRecord selectById(Connection conn, Long id) {
        String sql = "SELECT * FROM video_cache_record WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询缓存记录失败: id=" + id, e);
            throw new DatabaseException("查询缓存记录失败", e);
        }
    }

    /**
     * 查询视频缓存记录
     *
     * @param conn
     * @param videoId
     * @return
     */
    @Override
    public List<VideoCacheRecord> selectByVideoId(Connection conn, Long videoId) {
        String sql = "SELECT * FROM video_cache_record WHERE video_id=? ORDER BY create_time DESC";
        List<VideoCacheRecord> records = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, videoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                records.add(mapResultSetToEntity(rs));
            }
            return records;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询视频缓存记录失败: videoId=" + videoId, e);
            throw new DatabaseException("查询视频缓存记录失败", e);
        }
    }


    /**
     * 查询缓存类型记录
     *
     * @param conn
     * @param cacheType
     * @return
     */
    @Override
    public List<VideoCacheRecord> selectByCacheType(Connection conn, Integer cacheType) {
        String sql = "SELECT * FROM video_cache_record WHERE cache_type=? ORDER BY create_time DESC";
        List<VideoCacheRecord> records = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cacheType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                records.add(mapResultSetToEntity(rs));
            }
            return records;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询缓存类型记录失败: cacheType=" + cacheType, e);
            throw new DatabaseException("查询缓存类型记录失败", e);
        }
    }

    /**
     * 查询过期缓存记录
     *
     * @param conn
     * @return
     */
    @Override
    public List<VideoCacheRecord> selectExpiredRecords(Connection conn) {
        String sql = "SELECT * FROM video_cache_record WHERE expire_time IS NOT NULL AND expire_time < NOW()";
        List<VideoCacheRecord> records = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                records.add(mapResultSetToEntity(rs));
            }
            return records;
        } catch (SQLException e) {
            LogUtil.logError(logger, "查询过期缓存记录失败", e);
            throw new DatabaseException("查询过期缓存记录失败", e);
        }
    }

    /**
     * 删除视频所有缓存记录
     *
     * @param conn
     * @param videoId
     * @return
     */
    @Override
    public int deleteByVideoId(Connection conn, Long videoId) {
        String sql = "DELETE FROM video_cache_record WHERE video_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, videoId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.logError(logger, "删除视频所有缓存记录失败: videoId=" + videoId, e);
            throw new DatabaseException("删除视频所有缓存记录失败", e);
        }
    }

    private VideoCacheRecord mapResultSetToEntity(ResultSet rs) throws SQLException {
        VideoCacheRecord record = new VideoCacheRecord();
        record.setId(rs.getLong("id"));
        record.setVideoId(rs.getLong("video_id"));
        record.setCacheType(rs.getInt("cache_type"));
        record.setCachePath(rs.getString("cache_path"));
        record.setCacheUrl(rs.getString("cache_url"));
        record.setCacheSize(rs.getLong("cache_size"));

        Timestamp expireTime = rs.getTimestamp("expire_time");
        record.setExpireTime(expireTime != null ? expireTime.toLocalDateTime() : null);

        Timestamp createTime = rs.getTimestamp("create_time");
        record.setCreateTime(createTime != null ? createTime.toLocalDateTime() : null);

        Timestamp updateTime = rs.getTimestamp("update_time");
        record.setUpdateTime(updateTime != null ? updateTime.toLocalDateTime() : null);

        return record;
    }
}