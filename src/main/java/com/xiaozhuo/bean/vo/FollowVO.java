package com.xiaozhuo.bean.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FollowVO {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String nickname;
    private Long followerCount;
    private Long followingCount;
    private LocalDateTime createTime;
    private Boolean isFollowed;
}
