package com.xiaozhuo.entity;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_comment")
public class VideoComment {
    @Column(value = "id", isPrimaryKey = true, isAutoIncrement = true)
    private Long id;

    @Column("video_id")
    private Long videoId;

    @Column("user_id")
    private Long userId;

    @Column("parent_id")
    private Long parentId;

    @Column("content")
    private String content;

    @Column("like_count")
    private Long likeCount;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("is_delete")
    private Integer isDelete;
}