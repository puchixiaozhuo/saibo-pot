-- ============================================
-- 优惠券活动表增强 - 支持4种活动类型
-- ============================================

-- 1. 添加活动类型字段
ALTER TABLE coupon_activity
ADD COLUMN activity_type TINYINT NOT NULL DEFAULT 1 COMMENT '活动类型：0-无活动,1-普通抢购,2-分批次放券,3-预约抽签,4-观看解锁' AFTER video_id;

-- 2. 添加优惠内容字段
ALTER TABLE coupon_activity
ADD COLUMN discount_content VARCHAR(100) NOT NULL DEFAULT '' COMMENT '优惠内容描述，如：满100减20' AFTER description;

-- 3. 添加批次配置JSON字段
ALTER TABLE coupon_activity
ADD COLUMN batch_config TEXT COMMENT '分批次配置JSON，格式：[{"batchNumber":1,"stockCount":30,"releaseTime":"2026-05-02 10:00:00"}]' AFTER required_watch_seconds;

-- 4. 添加抽签配置JSON字段
ALTER TABLE coupon_activity
ADD COLUMN lottery_config TEXT COMMENT '抽签配置JSON，格式：{"lotteryTime":"2026-05-03 10:00:00","winnerCount":50}' AFTER batch_config;

-- 5. 添加索引
ALTER TABLE coupon_activity
ADD INDEX idx_activity_type (activity_type);

ALTER TABLE coupon_batch
    ADD COLUMN actual_release_time DATETIME COMMENT '实际释放时间' AFTER release_time;


-- 6. 创建批次表（如果不存在）
CREATE TABLE IF NOT EXISTS `coupon_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `batch_number` INT NOT NULL COMMENT '批次号',
  `stock_count` INT NOT NULL COMMENT '本批次库存数量',
  `released_stock` INT NOT NULL DEFAULT 0 COMMENT '已释放库存',
  `release_time` DATETIME NOT NULL COMMENT '计划释放时间',
  `actual_release_time` DATETIME COMMENT '实际释放时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待释放,1-已释放,2-已取消',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_release_time` (`release_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券批次表';

-- 7. 创建预约记录表（如果不存在）
CREATE TABLE IF NOT EXISTS `coupon_reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待抽签,1-中签,2-未中签',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`) COMMENT '防止重复预约',
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券预约记录表';
