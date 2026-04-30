package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("salt")
    private String salt;

    @Column("nickname")
    private String nickname;

    @Column("avatar")
    private String avatar;

    @Column("role")
    private Integer role;

    @Column("status")
    private Integer status;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("last_feed_read_time")
    private LocalDateTime lastFeedReadTime;
}
