package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_tag_relation")
public class VideoTagRelation {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("video_id")
    private Long videoId;

    @Column("tag_id")
    private Long tagId;

    @Column("create_time")
    private LocalDateTime createTime;
}