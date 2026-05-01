package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("coupon_batch")
public class CouponBatch {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("activity_id")
    private Long activityId;

    @Column("batch_number")
    private Integer batchNumber;

    @Column("stock_count")
    private Integer stockCount;

    @Column("released_stock")
    private Integer releasedStock;

    @Column("release_time")
    private LocalDateTime releaseTime;

    @Column("status")
    private Integer status;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
