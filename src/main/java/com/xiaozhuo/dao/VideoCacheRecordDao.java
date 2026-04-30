package com.xiaozhuo.dao;

import com.xiaozhuo.entity.VideoCacheRecord;

import java.sql.Connection;
import java.util.List;

public interface VideoCacheRecordDao {


    int insert(Connection conn, VideoCacheRecord record);

    int update(Connection conn, VideoCacheRecord record);

    int deleteById(Connection conn, Long id);

    VideoCacheRecord selectById(Connection conn, Long id);

    List<VideoCacheRecord> selectByVideoId(Connection conn, Long videoId);

    List<VideoCacheRecord> selectByCacheType(Connection conn, Integer cacheType);

    List<VideoCacheRecord> selectExpiredRecords(Connection conn);

    int deleteByVideoId(Connection conn, Long videoId);
}