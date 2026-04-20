package com.xiaozhuo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserToken {
    private Long id;
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expireTime;
    private LocalDateTime refreshExpireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
