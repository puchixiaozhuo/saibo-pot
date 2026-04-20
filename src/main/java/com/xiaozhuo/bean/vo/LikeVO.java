package com.xiaozhuo.bean.vo;

import lombok.Data;

@Data
public class LikeVO {
    private Long targetId;
    private Integer targetType;
    private Long likeCount;
    private Boolean isLiked;
}
