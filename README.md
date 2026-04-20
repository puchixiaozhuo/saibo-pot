# TopView2026 后端一轮考核项目 - 赛博聚石盆

## 项目简介
本项目是基于 **Java Servlet + JDBC + Maven** 开发的后端考核项目，实现了用户注册登录、视频管理、评论点赞等核心业务功能。这是一个单体架构的视频推流系统，遵循 MVC 分层结构，旨在打造一个"赛博搬石大王"的奇的收集与分享平台。

## 环境要求
- **JDK**：21（项目实际使用版本）
- **Maven**：3.6.x 及以上
- **Tomcat**：9.x（Servlet API 4.0.1）
- **数据库**：MySQL 8.0（兼容 5.7）
- **缓存**：redis实现缓存

- **开发工具**：IntelliJ IDEA 

## 技术栈

### 核心技术
- **后端框架**：Java Servlet（原生手写，无 Spring 系列）
- **数据库访问**：JDBC + 手写连接池
- **项目管理**：Maven
- **Web 容器**：Tomcat 9.0.111

### 依赖库
- `mysql-connector-java` 8.0.33 - MySQL 驱动
- `commons-codec` 1.15 - 密码加密（SHA256+ 盐）
- `fastjson` 1.2.83 - JSON 序列化
- `hutool-all` 5.8.23 - 工具包（含 JWT 支持）
- `lombok` 1.18.30 - 简化实体类开发
- `junit` 4.13.2 - 单元测试

## 项目结构
saibo-pot/ ├── src/main/ │ ├── java/com/xiaozhuo/ │ │ ├── bean/ │ │ │ ├── dto/ # 数据传输对象 │ │ │ │ ├── LoginDTO.java │ │ │ │ ├── UserDTO.java │ │ │ │ ├── VideoDTO.java │ │ │ │ └── CommentDTO.java │ │ │ └── vo/ # 视图对象 │ │ │ ├── LoginVO.java │ │ │ ├── UserVO.java │ │ │ ├── VideoVO.java │ │ │ └── CommentVO.java │ │ ├── constant/ # 常量定义 │ │ ├── entity/ # 实体类 │ │ │ ├── User.java │ │ │ ├── UserToken.java │ │ │ ├── VideoInfo.java │ │ │ ├── VideoComment.java │ │ │ ├── UserFollow.java │ │ │ └── UserLike.java │ │ ├── result/ # 统一返回结果 │ │ │ └── Result.java │ │ ├── service/ # 业务逻辑层（待实现） │ │ └── util/ # 工具类 │ │ ├── JDBCUtil.java # 数据库连接池 │ │ ├── MD5Util.java # 加密工具 │ │ ├── TokenUtil.java # Token 生成与校验 │ │ ├── JsonUtil.java # JSON 处理 │ │ └── LogUtil.java # 日志处理 │ ├── resources/ │ │ └── db.properties # 数据库配置 │ └── webapp/ │ ├── WEB-INF/ │ │ └── web.xml # Web 应用配置 │ └── index.html # 首页 ├── sql/ │ └── saibo.sql # 数据库建表语句 ├── pom.xml # Maven 配置 └── README.md # 项目说明文档
## 功能模块

### ✅ 必做功能
1. **注册功能**
   - 用户注册和管理员注册
   - 密码 SHA256+ 盐加密存储
   - 账号状态管理（正常/禁用）

2. **登录功能**
   - 账号密码登录
   - Token 生成与刷新机制
   - 登录状态缓存

3. **视频管理**
   - 视频列表查看
   - 视频详情查询
   - 视频信息缓存优化

4. **评论功能**
   - 视频评论发布
   - 评论删除
   - 评论列表展示

### 🚀 进阶功能
5. **关注功能**
   - 用户关注/取消关注
   - 关注列表查询
   - 粉丝数量统计

6. **点赞功能**
   - 视频点赞/取消
   - 评论点赞/取消
   - 热度统计与排序

7. **RBAC 权限管理**
   - 角色权限分配
   - 接口权限校验
   - 防止越权访问

## 数据库设计

### 核心表结构

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `sys_user` | 系统用户表 | id, username, password, salt, nickname, role |
| `user_token` | 用户 Token 表 | user_id, access_token, refresh_token, expire_time |
| `video_info` | 视频信息表 | author_id, title, video_url, view_count, like_count |
| `video_comment` | 视频评论表 | video_id, user_id, content, like_count |
| `user_follow` | 用户关注表 | user_id, follow_id |
| `user_like` | 通用点赞表 | user_id, target_type, target_id |
| `sys_permission` | 权限表 | perm_code, perm_name |
| `sys_role_perm` | 角色权限关联表 | role, perm_id |

详细 SQL 请查看：[`sql/saibo.sql`](sql/saibo.sql)

## 快速开始

### 1. 数据库初始化
## 功能模块

### ✅ 必做功能
1. **注册功能**
   - 用户注册和管理员注册
   - 密码 SHA256+ 盐加密存储
   - 账号状态管理（正常/禁用）
   - 将用户信息持久化到数据库中

2. **登录功能**
   - 账号密码登录
   - Token 生成与刷新机制
   - 校验用户是否登入，并缓存用户的登入状态

3. **视频管理**
   - 视频列表查看
   - 视频详情查询
   - 视频信息缓存优化

4. **评论功能**
   - 视频评论发布
   - 评论删除
   - 评论列表展示

### 🚀 进阶功能
5. **关注功能**
   - 用户关注/取消关注
   - 关注列表查询
   - 粉丝数量统计

6. **点赞功能**
   - 视频点赞/取消
   - 评论点赞/取消
   - 热度统计与排序展示评论

7. **RBAC 权限管理**
   - 角色权限分配
   - 接口权限校验
   - 防止越权访问

## 数据库设计

### 核心表结构

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `sys_user` | 系统用户表 | id, username, password, salt, nickname, role |
| `user_token` | 用户 Token 表 | user_id, access_token, refresh_token, expire_time |
| `video_info` | 视频信息表 | author_id, title, video_url, view_count, like_count |
| `video_comment` | 视频评论表 | video_id, user_id, content, like_count |
| `user_follow` | 用户关注表 | user_id, follow_id |
| `user_like` | 通用点赞表 | user_id, target_type, target_id |
| `sys_permission` | 权限表 | perm_code, perm_name |
| `sys_role_perm` | 角色权限关联表 | role, perm_id |

详细 SQL 请查看：[`sql/saibo.sql`](sql/saibo.sql)

## 快速开始

### 1. 数据库初始化
bash
登录 MySQL
mysql -u root -p
执行建表语句
source D:\saibo-pot\sql\saibo.sql###

### 2. 修改数据库配置
编辑 `src/main/resources/db.properties`：
properties jdbc.driver=com.mysql.cj.jdbc.Driver jdbc.url=jdbc:mysql://localhost:3306/cyber_stone_video?useSSL=false&serverTimezone=UTC&characterEncoding=utf8 jdbc.username=root jdbc.password=your_password jdbc.initialSize=5 jdbc.maxActive=20### 3. 编译打包

### 3. 编译打包
bash
cd D:\saibo-pot
mvn clean package

### 4. 部署到 Tomcat
bash
将生成的 war 包复制到 Tomcat 的 webapps 目录
copy target\saibo-pot.war C:\path\to\tomcat\webapps\
启动 Tomcat
C:\path\to\tomcat\bin\startup.bat### 5. 访问项目
- 项目地址：`http://localhost:9090/saibo-pot/`
- 默认管理员账号：`admin / 123456`

## API 接口文档

### 认证相关
| 接口 | 方法 | 描述 | 参数 |
|------|------|------|------|
| `/api/user/register` | POST | 用户注册 | username, password, nickname |
| `/api/user/login` | POST | 用户登录 | username, password |
| `/api/user/refresh` | POST | Token 刷新 | refresh_token |
| `/api/user/logout` | POST | 用户登出 | access_token |

### 视频相关
| 接口 | 方法 | 描述 | 参数 |
|------|------|------|------|
| `/api/video/list` | GET | 获取视频列表 | page, size, sortType |
| `/api/video/detail` | GET | 获取视频详情 | videoId |
| `/api/video/add` | POST | 发布视频 | title, videoUrl, cover, description |
| `/api/video/delete` | DELETE | 删除视频 | videoId |

### 评论相关
| 接口 | 方法 | 描述 | 参数 |
|------|------|------|------|
| `/api/comment/list` | GET | 获取评论列表 | videoId, page, size |
| `/api/comment/add` | POST | 发布评论 | videoId, content |
| `/api/comment/delete` | DELETE | 删除评论 | commentId |

### 互动相关
| 接口 | 方法 | 描述 | 参数 |
|------|------|------|------|
| `/api/follow/follow` | POST | 关注用户 | followId |
| `/api/follow/list` | GET | 关注列表 | userId |
| `/api/like/like` | POST | 点赞 | targetType, targetId |

> ⚠️ **注意**：所有需要登录的接口需要在 Header 中携带 `Authorization: Bearer {access_token}`

## 核心特性

### 1. 手写数据库连接池
- 基于 JDBC 实现简单的连接池管理
- 支持连接复用，提升性能
- 配置最小/最大连接数

### 2. ORM 基础封装
- 借鉴 MyBatis 思路封装 CRUD
- 实体类与数据库表映射
- 简化数据库操作代码

### 3. 日志统一处理
- 使用 JDK 自带 logging 包
- 记录关键操作日志
- 支持不同日志级别（INFO/WARNING/SEVERE）

### 4. 安全机制
- 密码 SHA256+ 随机盐加密
- Token 双重验证（Access + Refresh）
- RBAC 权限控制

## 开发规范

### 代码规范
- 变量命名：camelCase 驼峰命名
- 类命名：PascalCase 大驼峰
- 常量命名：UPPER_CASE 全大写
- 注释完整，逻辑清晰

### 分层规范
- **Controller 层**：接收请求，参数校验
- **Service 层**：业务逻辑处理
- **DAO 层**：数据库操作
- **Entity 层**：数据实体
- **DTO/VO 层**：数据传输/视图对象

## 常见问题

### Q1: Tomcat 启动失败？
检查端口 9090 是否被占用，或修改 `tomcat/conf/server.xml` 中的端口配置。

### Q2: 数据库连接失败？
确认 MySQL 服务已启动，检查 `db.properties` 配置是否正确。

### Q3: 中文乱码问题？
确保数据库字符集为 `utf8mb4`，并在 JDBC URL 中添加 `characterEncoding=utf8`。

## 参考资料
- [Java Servlet 官方文档](https://jakarta.ee/specifications/servlet/)
- [JDBC API 教程](https://docs.oracle.com/javase/tutorial/jdbc/)
- [Maven 官方指南](https://maven.apache.org/guides/)

## 版权说明
本项目为 TopView2026 后端一轮考核作业，仅供学习交流使用。

---
**项目名称**：赛博聚石盆  
**开发者**：xiaozhuo  
**考核时间**：2024.3.26 - 2024.4.20  
**口号**：让赤石的乐趣重新盛行！🪨✨
