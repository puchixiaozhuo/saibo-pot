package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoComment {
    private Long id;
    private Long videoId;
    private Long userId;
    private Long parentId;
    private String content;
    private Long likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDelete;
}
