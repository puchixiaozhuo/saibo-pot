package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoInfo {
    private Long id;
    private Long authorId;
    private String title;
    private String cover;
    private String videoUrl;
    private Integer duration;
    private Long fileSize;
    private String format;
    private String resolution;
    private Integer cacheStatus;
    private Integer transcodeStatus;
    private Long categoryId;
    private String description;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDelete;
}

