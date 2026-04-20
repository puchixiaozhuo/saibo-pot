package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserLike {
    private Long id;
    private Long userId;
    private Integer targetType;
    private Long targetId;
    private LocalDateTime createTime;
}
