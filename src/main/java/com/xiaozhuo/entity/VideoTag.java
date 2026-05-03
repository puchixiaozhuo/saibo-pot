package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_tag")
public class VideoTag {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("tag_name")
    private String tagName;

    @Column("use_count")
    private Long useCount;

    @Column("create_time")
    private LocalDateTime createTime;
}
