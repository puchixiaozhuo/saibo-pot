USE cyber_stone_video;

-- ============================================
-- 1. 创建 2026 年各月的评论分表
-- ============================================
CREATE TABLE IF NOT EXISTS video_comment_202601 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202602 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202603 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202604 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202605 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202606 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202607 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202608 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202609 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202610 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202611 LIKE video_comment;
CREATE TABLE IF NOT EXISTS video_comment_202612 LIKE video_comment;

-- ============================================
-- 2. 迁移现有数据（示例：将 5 月数据迁入）
-- ============================================
INSERT INTO video_comment_202605
SELECT * FROM video_comment
WHERE create_time >= '2026-05-01 00:00:00' AND create_time < '2026-06-01 00:00:00';

-- ============================================
-- 3. 验证迁移结果
-- ============================================
SELECT
    (SELECT COUNT(*) FROM video_comment) AS main_table_count,
    (SELECT COUNT(*) FROM video_comment_202605) AS may_table_count;
