package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Permission {
    private Long id;
    private String permCode;
    private String permName;
    private LocalDateTime createTime;
}

