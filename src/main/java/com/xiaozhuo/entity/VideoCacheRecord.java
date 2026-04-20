package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoCacheRecord {
    private Long id;
    private Long videoId;
    private Integer cacheType;
    private String cachePath;
    private String cacheUrl;
    private Long cacheSize;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
