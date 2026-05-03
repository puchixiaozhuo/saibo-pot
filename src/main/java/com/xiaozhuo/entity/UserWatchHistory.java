package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_watch_history")
public class UserWatchHistory {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("video_id")
    private Long videoId;

    @Column("watch_progress")
    private Integer watchProgress;

    @Column("watch_duration")
    private Integer watchDuration;

    @Column("watch_time")
    private LocalDateTime watchTime;
}