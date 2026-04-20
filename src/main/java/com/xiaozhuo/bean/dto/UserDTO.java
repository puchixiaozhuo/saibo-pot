package com.xiaozhuo.bean.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String password;
    private String nickname;
    private Integer type;
}

