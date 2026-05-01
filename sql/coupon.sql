-- ============================================
-- 优惠券抢购模块 - 数据库表设计
-- ============================================

-- 1. 优惠券活动表
CREATE TABLE IF NOT EXISTS `coupon_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `video_id` BIGINT NOT NULL COMMENT '关联视频ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '活动描述',
  `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
  `remaining_stock` INT NOT NULL DEFAULT 0 COMMENT '剩余库存（冗余字段，用于展示）',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未开始，1-进行中，2-已结束',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_id` (`video_id`) COMMENT '视频ID索引',
  KEY `idx_status` (`status`) COMMENT '状态索引',
  KEY `idx_time` (`start_time`, `end_time`) COMMENT '时间范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券活动表';

-- 2. 用户优惠券表
CREATE TABLE IF NOT EXISTS `coupon_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `coupon_code` VARCHAR(32) NOT NULL COMMENT '优惠券码',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已过期',
  `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`) COMMENT '用户-活动唯一索引（防止重复领取）',
  KEY `idx_coupon_code` (`coupon_code`) COMMENT '优惠券码索引',
  KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
  KEY `idx_activity_id` (`activity_id`) COMMENT '活动ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 3. 插入测试数据
INSERT INTO `coupon_activity`
(`video_id`, `title`, `description`, `total_stock`, `remaining_stock`, `start_time`, `end_time`, `status`, `version`)
VALUES
(1, '限时优惠券', '满100减20元优惠券', 100, 100, '2026-04-30 20:00:00', '2026-04-30 22:00:00', 1, 0);

-- 添加观看时长要求字段
ALTER TABLE coupon_activity
    ADD COLUMN required_watch_seconds INT DEFAULT 0 COMMENT '需要观看的时长（秒），0表示无需观看';

-- 添加观看记录表
CREATE TABLE IF NOT EXISTS `user_video_watch_record` (
                                                         `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                                         `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                                         `video_id` BIGINT NOT NULL COMMENT '视频ID',
                                                         `activity_id` BIGINT NOT NULL COMMENT '关联的活动ID',
                                                         `watched_seconds` INT NOT NULL DEFAULT 0 COMMENT '已观看时长（秒）',
                                                         `is_unlocked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解锁：0-未解锁，1-已解锁',
                                                         `unlock_time` DATETIME COMMENT '解锁时间',
                                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                         UNIQUE KEY `uk_user_video_activity` (`user_id`, `video_id`, `activity_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_activity_id` (`activity_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户视频观看记录表';