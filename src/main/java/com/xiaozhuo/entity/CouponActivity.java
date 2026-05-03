package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("coupon_activity")
public class CouponActivity {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("video_id")
    private Long videoId;

    @Column("activity_type")
    private Integer activityType;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("discount_content")
    private String discountContent;

    @Column("total_stock")
    private Integer totalStock;

    @Column("remaining_stock")
    private Integer remainingStock;

    @Column("start_time")
    private LocalDateTime startTime;

    @Column("end_time")
    private LocalDateTime endTime;

    @Column("status")
    private Integer status;

    @Column("version")
    private Integer version;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("required_watch_seconds")
    private Integer requiredWatchSeconds;

    @Column("batch_config")
    private String batchConfig;

    @Column("lottery_config")
    private String lotteryConfig;
}