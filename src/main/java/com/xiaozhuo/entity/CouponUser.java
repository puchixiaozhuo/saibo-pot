package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("coupon_user")
public class CouponUser {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("activity_id")
    private Long activityId;

    @Column("user_id")
    private Long userId;

    @Column("coupon_code")
    private String couponCode;

    @Column("status")
    private Integer status;

    @Column("use_time")
    private LocalDateTime useTime;

    @Column("expire_time")
    private LocalDateTime expireTime;

    @Column("create_time")
    private LocalDateTime createTime;
}