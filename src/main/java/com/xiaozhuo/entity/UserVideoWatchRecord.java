package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_video_watch_record")
public class UserVideoWatchRecord {

    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("video_id")
    private Long videoId;

    @Column("activity_id")
    private Long activityId;

    @Column("watched_seconds")
    private Integer watchedSeconds;

    @Column("is_unlocked")
    private Integer isUnlocked;

    @Column("unlock_time")
    private LocalDateTime unlockTime;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
