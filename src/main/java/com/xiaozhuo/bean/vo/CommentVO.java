package com.xiaozhuo.bean.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long videoId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long parentId;
    private String content;
    private Long likeCount;
    private LocalDateTime createTime;
    private Boolean isLiked;
}
