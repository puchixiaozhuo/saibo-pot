package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_category")
public class VideoCategory {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("category_name")
    private String categoryName;

    @Column("parent_id")
    private Long parentId;

    @Column("sort_order")
    private Integer sortOrder;

    @Column("is_delete")
    private Integer isDelete;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
