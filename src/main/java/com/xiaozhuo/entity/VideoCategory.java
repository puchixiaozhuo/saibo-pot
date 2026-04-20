package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoCategory {
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private Integer isDelete;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
