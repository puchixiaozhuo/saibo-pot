/* 数据库创建与使用 */
CREATE DATABASE IF NOT EXISTS cyber_stone_video DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cyber_stone_video;

/* 1. 用户表：存储普通用户/管理员基础信息，区分角色，密码哈希加盐存储 */
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一标识',
                          username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号，唯一',
                          password CHAR(64) NOT NULL COMMENT '密码，SHA256+盐加密存储',
                          salt CHAR(16) NOT NULL COMMENT '密码加密盐值，随机生成',
                          nickname VARCHAR(50) NOT NULL COMMENT '用户昵称/博主名',
                          avatar VARCHAR(255) DEFAULT '/default/avatar.png' COMMENT '用户头像地址',
                          role TINYINT NOT NULL DEFAULT 0 COMMENT '用户角色：0-普通用户，1-管理员',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-禁用',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间/注册时间',
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          INDEX idx_username (username),
                          INDEX idx_role (role)
) COMMENT = '系统用户表（含管理员/博主）';

-- 在 sys_user 表中添加最后读取Feed时间字段
ALTER TABLE `sys_user`
    ADD COLUMN `last_feed_read_time` DATETIME NULL DEFAULT NULL COMMENT '最后读取Feed时间' AFTER `update_time`;

-- 为已有用户初始化（设置为当前时间，表示所有历史内容都已读）
UPDATE `sys_user` SET `last_feed_read_time` = NOW();

/* 2. 用户登录Token表：存储登录Token，支持刷新机制，缓存登录状态关联 */
DROP TABLE IF EXISTS user_token;
CREATE TABLE user_token (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Token记录ID',
                            user_id BIGINT NOT NULL COMMENT '关联用户ID',
                            access_token CHAR(64) NOT NULL UNIQUE COMMENT '访问Token，唯一',
                            refresh_token CHAR(64) NOT NULL UNIQUE COMMENT '刷新Token，唯一',
                            expire_time DATETIME NOT NULL COMMENT 'access_token过期时间',
                            refresh_expire_time DATETIME NOT NULL COMMENT 'refresh_token过期时间',
                            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Token生成时间',
                            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Token刷新时间',
                            FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                            INDEX idx_user_id (user_id),
                            INDEX idx_access_token (access_token),
                            INDEX idx_refresh_token (refresh_token)
) COMMENT = '用户登录Token表，支持Token刷新';

/* 3. 视频表：存储视频核心信息，用于视频管理与缓存，预留热度字段 */
DROP TABLE IF EXISTS video_info;
CREATE TABLE video_info (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '视频唯一标识',
                            author_id BIGINT NOT NULL COMMENT '视频发布者/博主ID，关联用户表',
                            title VARCHAR(100) NOT NULL COMMENT '视频标题',
                            cover VARCHAR(255) DEFAULT '/default/cover.png' COMMENT '视频封面地址',
                            video_url VARCHAR(255) NOT NULL COMMENT '视频播放地址',
                            description TEXT COMMENT '视频描述/简介',
                            view_count BIGINT NOT NULL DEFAULT 0 COMMENT '视频播放量',
                            like_count BIGINT NOT NULL DEFAULT 0 COMMENT '视频点赞数',
                            comment_count BIGINT NOT NULL DEFAULT 0 COMMENT '视频评论数',
                            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
                            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除状态：0-未删，1-已删',
                            FOREIGN KEY (author_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                            INDEX idx_author_id (author_id),
                            INDEX idx_create_time (create_time),
                            INDEX idx_like_count (like_count) COMMENT '热度排序索引'
) COMMENT = '视频信息表，核心视频管理表';

-- ============================================
-- 1. 优化 video_info 表 - 新增视频元数据字段
-- ============================================
ALTER TABLE video_info
    ADD COLUMN duration INT DEFAULT 0 COMMENT '视频时长（秒）' AFTER video_url,
ADD COLUMN file_size BIGINT DEFAULT 0 COMMENT '视频文件大小（字节）' AFTER duration,
ADD COLUMN format VARCHAR(20) DEFAULT 'mp4' COMMENT '视频格式（mp4/avi/mov等）' AFTER file_size,
ADD COLUMN resolution VARCHAR(20) DEFAULT '1080p' COMMENT '视频分辨率（720p/1080p/4k）' AFTER format,
ADD COLUMN cache_status TINYINT DEFAULT 0 COMMENT '缓存状态：0-未缓存，1-已缓存到本地，2-已缓存到CDN' AFTER resolution,
ADD COLUMN transcode_status TINYINT DEFAULT 0 COMMENT '转码状态：0-未转码，1-转码中，2-转码完成，3-转码失败' AFTER cache_status,
ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '视频分类ID' AFTER transcode_status,
ADD INDEX idx_cache_status (cache_status),
ADD INDEX idx_category_id (category_id);

-- 在 video_info 表中添加收藏数字段
ALTER TABLE `video_info`
    ADD COLUMN `favorite_count` BIGINT NOT NULL DEFAULT 0 COMMENT '收藏数量' AFTER `comment_count`;


-- ============================================
-- 2. 创建视频分类表
-- ============================================
DROP TABLE IF EXISTS video_category;
CREATE TABLE video_category (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
                                category_name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
                                parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0为一级分类',
                                sort_order INT DEFAULT 0 COMMENT '排序权重',
                                is_delete TINYINT DEFAULT 0 COMMENT '删除状态：0-正常，1-已删',
                                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                INDEX idx_parent_id (parent_id)
) COMMENT = '视频分类表';

-- 初始化视频分类数据
INSERT INTO video_category (category_name, parent_id, sort_order) VALUES
                                                                      ('搞笑', 0, 1),
                                                                      ('游戏', 0, 2),
                                                                      ('音乐', 0, 3),
                                                                      ('科技', 0, 4),
                                                                      ('生活', 0, 5),
                                                                      ('教育', 0, 6);

-- ============================================
-- 3. 创建视频标签表（多对多关系）
-- ============================================
DROP TABLE IF EXISTS video_tag;
CREATE TABLE video_tag (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
                           tag_name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
                           use_count BIGINT DEFAULT 0 COMMENT '使用次数',
                           create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           INDEX idx_tag_name (tag_name)
) COMMENT = '视频标签表';

-- ============================================
-- 4. 创建视频-标签关联表
-- ============================================
DROP TABLE IF EXISTS video_tag_relation;
CREATE TABLE video_tag_relation (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
                                    video_id BIGINT NOT NULL COMMENT '视频ID',
                                    tag_id BIGINT NOT NULL COMMENT '标签ID',
                                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    FOREIGN KEY (video_id) REFERENCES video_info(id) ON DELETE CASCADE,
                                    FOREIGN KEY (tag_id) REFERENCES video_tag(id) ON DELETE CASCADE,
                                    UNIQUE INDEX uk_video_tag (video_id, tag_id) COMMENT '防止重复关联',
                                    INDEX idx_tag_id (tag_id) COMMENT '查询某标签下所有视频'
) COMMENT = '视频标签关联表';

-- ============================================
-- 5. 创建用户观看历史表
-- ============================================
DROP TABLE IF EXISTS user_watch_history;
CREATE TABLE user_watch_history (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '历史记录ID',
                                    user_id BIGINT NOT NULL COMMENT '用户ID',
                                    video_id BIGINT NOT NULL COMMENT '视频ID',
                                    watch_progress INT DEFAULT 0 COMMENT '观看进度（秒）',
                                    watch_duration INT DEFAULT 0 COMMENT '本次观看时长（秒）',
                                    watch_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '观看时间',
                                    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                                    FOREIGN KEY (video_id) REFERENCES video_info(id) ON DELETE CASCADE,
                                    INDEX idx_user_id (user_id),
                                    INDEX idx_watch_time (watch_time),
                                    UNIQUE INDEX uk_user_video (user_id, video_id) COMMENT '同一用户同一视频只保留最新记录'
) COMMENT = '用户观看历史表';

-- ============================================
-- 6. 创建视频收藏表
-- ============================================
DROP TABLE IF EXISTS user_favorite;
CREATE TABLE user_favorite (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
                               user_id BIGINT NOT NULL COMMENT '用户ID',
                               video_id BIGINT NOT NULL COMMENT '视频ID',
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                               FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                               FOREIGN KEY (video_id) REFERENCES video_info(id) ON DELETE CASCADE,
                               UNIQUE INDEX uk_user_video (user_id, video_id) COMMENT '防止重复收藏',
                               INDEX idx_user_id (user_id)
) COMMENT = '用户视频收藏表';

-- ============================================
-- 7. 创建视频缓存记录表（用于缓存管理）
-- ============================================
DROP TABLE IF EXISTS video_cache_record;
CREATE TABLE video_cache_record (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '缓存记录ID',
                                    video_id BIGINT NOT NULL COMMENT '视频ID',
                                    cache_type TINYINT NOT NULL COMMENT '缓存类型：1-本地缓存，2-CDN缓存',
                                    cache_path VARCHAR(255) COMMENT '缓存文件路径（本地缓存时有效）',
                                    cache_url VARCHAR(255) COMMENT '缓存访问URL（CDN缓存时有效）',
                                    cache_size BIGINT DEFAULT 0 COMMENT '缓存文件大小（字节）',
                                    expire_time DATETIME COMMENT '缓存过期时间',
                                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '缓存时间',
                                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    FOREIGN KEY (video_id) REFERENCES video_info(id) ON DELETE CASCADE,
                                    INDEX idx_video_id (video_id),
                                    INDEX idx_expire_time (expire_time)
) COMMENT = '视频缓存记录表';

-- ============================================
-- 8. 更新权限表 - 添加新权限
-- ============================================
INSERT INTO sys_permission (perm_code, perm_name) VALUES
                                                      ('video:upload', '视频上传'),
                                                      ('video:download', '视频下载'),
                                                      ('video:cache', '视频缓存管理'),
                                                      ('video:category', '视频分类管理'),
                                                      ('history:view', '观看历史查看'),
                                                      ('favorite:manage', '收藏管理');

-- 管理员拥有所有新权限
INSERT INTO sys_role_perm (role, perm_id)
SELECT 1, id FROM sys_permission
WHERE perm_code IN ('video:upload', 'video:download', 'video:cache', 'video:category', 'history:view', 'favorite:manage')
  AND id NOT IN (SELECT perm_id FROM sys_role_perm WHERE role = 1);

/* 4. 评论表：存储视频评论，支持评论点赞、按时间/热度排序 */
DROP TABLE IF EXISTS video_comment;
CREATE TABLE video_comment (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论唯一标识',
                               video_id BIGINT NOT NULL COMMENT '关联视频ID',
                               user_id BIGINT NOT NULL COMMENT '评论发布者ID',
                               parent_id BIGINT DEFAULT 0 COMMENT '父评论ID，0为根评论（暂不做回复，预留）',
                               content TEXT NOT NULL COMMENT '评论内容',
                               like_count BIGINT NOT NULL DEFAULT 0 COMMENT '评论点赞数',
                               create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
                               update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除状态：0-未删，1-已删',
                               FOREIGN KEY (video_id) REFERENCES video_info(id) ON DELETE CASCADE,
                               FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                               INDEX idx_video_id (video_id),
                               INDEX idx_user_id (user_id),
                               INDEX idx_create_time (create_time),
                               INDEX idx_like_count (like_count) COMMENT '评论热度排序索引'
) COMMENT = '视频评论表，支持删除、点赞统计';

/* 5. 关注表：存储用户关注关系，支持查询关注列表/粉丝数 */
DROP TABLE IF EXISTS user_follow;
CREATE TABLE user_follow (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注记录ID',
                             user_id BIGINT NOT NULL COMMENT '关注者ID（粉丝）',
                             follow_id BIGINT NOT NULL COMMENT '被关注者ID（博主/用户）',
                             create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
                             FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                             FOREIGN KEY (follow_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                             UNIQUE INDEX uk_user_follow (user_id, follow_id) COMMENT '唯一索引，防止重复关注',
                             INDEX idx_follow_id (follow_id) COMMENT '查询粉丝数索引'
) COMMENT = '用户关注关系表，进阶功能-关注/粉丝统计';

/* 6. 点赞表：通用点赞表，存储视频/评论的点赞记录，防止重复点赞 */
DROP TABLE IF EXISTS user_like;
CREATE TABLE user_like (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞记录ID',
                           user_id BIGINT NOT NULL COMMENT '点赞者ID',
                           target_type TINYINT NOT NULL COMMENT '点赞目标类型：1-视频，2-评论',
                           target_id BIGINT NOT NULL COMMENT '点赞目标ID（关联video_info/id或video_comment/id）',
                           create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                           FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                           UNIQUE INDEX uk_user_like (user_id, target_type, target_id) COMMENT '唯一索引，防止重复点赞',
                           INDEX idx_target (target_type, target_id) COMMENT '统计点赞数索引'
) COMMENT = '通用点赞表，进阶功能-视频/评论点赞';

/* 7. 权限表：RBAC基础-接口权限表，为每个接口分配唯一权限 */
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限唯一标识',
                                perm_code VARCHAR(50) NOT NULL UNIQUE COMMENT '权限编码（如：video:add，user:delete）',
                                perm_name VARCHAR(50) NOT NULL COMMENT '权限名称（如：视频新增，用户删除）',
                                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                INDEX idx_perm_code (perm_code)
) COMMENT = '系统权限表，进阶功能-RBAC权限管理';

/* 8. 角色权限关联表：RBAC-角色绑定权限，一对多关系 */
DROP TABLE IF EXISTS sys_role_perm;
CREATE TABLE sys_role_perm (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联记录ID',
                               role TINYINT NOT NULL COMMENT '角色类型：0-普通用户，1-管理员',
                               perm_id BIGINT NOT NULL COMMENT '关联权限ID',
                               FOREIGN KEY (perm_id) REFERENCES sys_permission(id) ON DELETE CASCADE,
                               UNIQUE INDEX uk_role_perm (role, perm_id) COMMENT '唯一索引，防止角色重复绑定权限'
) COMMENT = '角色权限关联表，进阶功能-RBAC角色权限分配';

/* 初始化基础数据 */
-- 初始化角色：普通用户(0)、管理员(1) 基础权限（示例）
INSERT INTO sys_permission (perm_code, perm_name) VALUES
                                                      ('user:register', '用户注册'),
                                                      ('user:login', '用户登录'),
                                                      ('video:view', '视频查看'),
                                                      ('comment:add', '评论新增'),
                                                      ('comment:delete', '评论删除'),
                                                      ('admin:user:manage', '管理员用户管理'),
                                                      ('admin:video:manage', '管理员视频管理');

-- 普通用户权限绑定
INSERT INTO sys_role_perm (role, perm_id) SELECT 0, id FROM sys_permission WHERE perm_code IN ('user:register', 'user:login', 'video:view', 'comment:add', 'comment:delete');
-- 管理员权限绑定（包含普通用户+管理员专属）
INSERT INTO sys_role_perm (role, perm_id) SELECT 1, id FROM sys_permission;

-- 初始化一个默认管理员账号：admin / 123456（盐：1234567890abcdef，SHA256加密后：f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8）
INSERT INTO sys_user (username, password, salt, nickname, role) VALUES
    ('admin', 'f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8', '1234567890abcdef', '赛博管理员', 1);

-- ============================================
-- RBAC 权限管理 - 完整权限配置
-- ============================================
-- 1. 清空旧的权限数据（如果已存在）
DELETE FROM sys_role_perm;
DELETE FROM sys_permission;

-- 2. 插入完整的权限列表
INSERT INTO sys_permission (perm_code, perm_name) VALUES
                                                      -- 用户相关权限
                                                      ('user:register', '用户注册'),
                                                      ('user:login', '用户登录'),
                                                      ('user:info', '查看用户信息'),

                                                      -- 视频相关权限
                                                      ('video:view', '视频查看'),
                                                      ('video:upload', '视频上传'),
                                                      ('video:delete:own', '删除自己的视频'),
                                                      ('video:delete:any', '删除任意视频'),
                                                      ('video:like', '视频点赞/取消点赞'),
                                                      ('video:download', '视频下载'),
                                                      ('video:cache', '视频缓存'),

                                                      -- 评论相关权限
                                                      ('comment:add', '发表评论'),
                                                      ('comment:delete:own', '删除自己的评论'),
                                                      ('comment:delete:any', '删除任意评论'),
                                                      ('comment:like', '评论点赞'),

                                                      -- 关注相关权限
                                                      ('follow:manage', '关注/取消关注'),
                                                      ('follow:view', '查看关注列表'),

                                                      -- 管理员专属权限
                                                      ('admin:user:manage', '用户管理'),
                                                      ('admin:video:manage', '视频管理'),
                                                      ('admin:comment:manage', '评论管理'),
                                                      ('admin:permission:manage', '权限管理');

-- 3. 普通用户权限绑定（role=0）
INSERT INTO sys_role_perm (role, perm_id)
SELECT 0, id FROM sys_permission
WHERE perm_code IN (
                    'user:register',
                    'user:login',
                    'user:info',
                    'video:view',
                    'video:upload',
                    'video:delete:own',
                    'video:like',
                    'video:download',
                    'comment:add',
                    'comment:delete:own',
                    'comment:like',
                    'follow:manage',
                    'follow:view'
    );

-- 4. 管理员权限绑定（role=1，拥有所有权限）
INSERT INTO sys_role_perm (role, perm_id)
SELECT 1, id FROM sys_permission;

-- 5. 验证权限配置
SELECT '普通用户权限列表：' as info;
SELECT p.perm_code, p.perm_name
FROM sys_permission p
         INNER JOIN sys_role_perm rp ON p.id = rp.perm_id
WHERE rp.role = 0
ORDER BY p.id;

SELECT '管理员权限列表：' as info;
SELECT p.perm_code, p.perm_name
FROM sys_permission p
         INNER JOIN sys_role_perm rp ON p.id = rp.perm_id
WHERE rp.role = 1
ORDER BY p.id;