package com.xiaozhuo.bean.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoVO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String cover;
    private String videoUrl;
    private String description;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private LocalDateTime createTime;
    private Boolean isLiked;
}
