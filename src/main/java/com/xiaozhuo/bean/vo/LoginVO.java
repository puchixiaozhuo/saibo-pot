package com.xiaozhuo.bean.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long userId;
    private String username;
    private String nickname;
    private UserVO userInfo;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
