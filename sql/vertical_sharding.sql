-- ============================================
-- 垂直分表实施方案
-- 目标：将 video_info 表的大字段 description 分离到独立表
-- ============================================

USE cyber_stone_video;

-- 1. 创建视频详情扩展表
CREATE TABLE IF NOT EXISTS video_info_detail (
    video_id BIGINT PRIMARY KEY COMMENT '视频ID（关联 video_info.id）',
    description TEXT COMMENT '视频详细描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (video_id) REFERENCES video_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT = '视频详情扩展表（垂直分表）';

-- 2. 迁移现有 description 数据到新表
INSERT INTO video_info_detail (video_id, description)
SELECT id, description FROM video_info
WHERE description IS NOT NULL AND description != ''
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 3. 验证迁移结果
SELECT
    (SELECT COUNT(*) FROM video_info) AS total_videos,
    (SELECT COUNT(*) FROM video_info_detail) AS videos_with_description;

-- 4. （可选）从原表删除 description 字段
-- ⚠️ 注意：生产环境建议保留字段但设为 NULL，避免兼容性问题
-- ALTER TABLE video_info DROP COLUMN description;
