package com.xiaozhuo.bean.dto;

import lombok.Data;

@Data
public class CommentDTO {
    private Long videoId;
    private Long parentId;
    private String content;
}
