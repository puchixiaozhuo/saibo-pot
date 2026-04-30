package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_info")
public class VideoInfo {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("author_id")
    private Long authorId;

    @Column("title")
    private String title;

    @Column("cover")
    private String cover;

    @Column("video_url")
    private String videoUrl;

    @Column("duration")
    private Integer duration;

    @Column("file_size")
    private Long fileSize;

    @Column("format")
    private String format;

    @Column("resolution")
    private String resolution;

    @Column("cache_status")
    private Integer cacheStatus;

    @Column("transcode_status")
    private Integer transcodeStatus;

    @Column("category_id")
    private Long categoryId;

    @Column("description")
    private String description;

    @Column("view_count")
    private Long viewCount;

    @Column("like_count")
    private Long likeCount;

    @Column("comment_count")
    private Long commentCount;

    @Column("favorite_count")
    private Long favoriteCount;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("is_delete")
    private Integer isDelete;
}


