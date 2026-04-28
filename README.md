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


### 1. 安全机制
- 密码 SHA256+ 随机盐加密
- Token 双重验证（Access + Refresh）
- RBAC 权限控制

## 开发规范

### 测试验证
- 每个模块完成后编写单元测试
- 使用控制台输出快速验证核心逻辑（**所有测试输出必须使用英文**）
- 模拟异常场景测试容错能力


### 代码规范
- 变量命名：camelCase 驼峰命名
- 类命名：PascalCase 大驼峰
- 常量命名：UPPER_CASE 全大写
- 注释完整，逻辑清晰
- 遵循 Java 命名规范（驼峰命名）
- 所有公共方法必须添加注释
- 保持代码简洁，单一职责原则
- **测试日志与控制台输出统一使用英文**

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

# TopView2026 - 技术实现要求

## 📋 项目概述

本项目是一个基于 Java Servlet 的视频管理平台后端系统，需要实现以下核心技术模块。

---

## 🔧 必做功能

### 1. MySQL 数据库连接池

**目标**: 自主实现一个轻量级数据库连接池，提升数据库访问性能。

**核心要求**:
- 实现连接的创建、获取、归还机制
- 支持连接池大小配置（最小连接数、最大连接数）
- 实现连接有效性检测与自动回收
- 支持多线程环境下的安全访问

**技术要点**:
- 使用 `java.util.concurrent` 包管理线程安全
- 实现连接的复用机制，避免频繁创建/销毁连接
- 提供连接超时处理与异常恢复机制

---

### 2. CRUD 操作封装（类 ORM 实现）

**目标**: 借鉴 ORM 框架思想，封装通用的数据库增删改查操作。

**核心要求**:
- 实现基于实体类的自动化 SQL 生成
- 提供通用的 `insert`、`delete`、`update`、`select` 方法
- 支持条件查询与分页查询
- 实现对象与数据库记录的自动映射

**技术要点**:
- 使用 Java 反射机制解析实体类字段
- 通过注解或命名规范映射表名与字段名
- 封装 `PreparedStatement` 防止 SQL 注入
- 提供灵活的查询条件构建器

---

### 3. 日志统一处理

**目标**: 使用 JDK 自带日志系统（`java.util.logging`）实现统一的日志管理。

**核心要求**:
- 支持不同日志等级（`SEVERE`、`WARNING`、`INFO`、`CONFIG`、`FINE`）
- 在关键位置记录日志：
  - 数据库连接建立与释放
  - SQL 执行异常
  - 数据验证失败
  - 事务开始、提交、回滚
  - 业务逻辑异常
- 实现日志格式化输出（时间戳、类名、方法名、日志内容）
- 支持日志文件滚动存储

**技术要点**:
- 了解面向切面编程（AOP）思想
- 探索动态代理（CGLIB/JDK Proxy）实现日志拦截
- 设计统一的日志工具类 `LogUtil`
- 配置日志级别与输出策略

---

## 🚀 进阶功能

### 4. 统一 Bean 对象管理（IoC 容器）

**目标**: 实现一个简单的控制反转（IoC）容器，统一管理应用中的 Bean 对象。

**核心要求**:
- 实现 Bean 的注册、获取与管理
- 支持单例（Singleton）与原型（Prototype）模式
- 实现依赖注入（DI）功能
- 支持 Bean 的生命周期管理（初始化、销毁）

**技术要点**:
- 使用 `ConcurrentHashMap` 存储 Bean 实例
- 通过反射机制创建对象实例
- 实现构造器注入与 Setter 注入
- 支持注解扫描与自动装配（可选）

---

### 5. 统一异常处理

**目标**: 集中管理系统中的各类异常，提供友好的错误响应。

**核心要求**:
- 定义自定义异常体系（业务异常、数据异常、权限异常等）
- 实现全局异常处理器
- 统一异常日志记录
- 返回标准化的错误响应格式

**技术要点**:
- 设计异常层次结构（继承 `Exception` 或 `RuntimeException`）
- 使用 Servlet Filter 或 Handler 捕获异常
- 将异常信息转换为标准 JSON 响应
- 区分开发环境与生产环境的错误信息展示

---

### 6. 事务回滚处理

**目标**: 实现数据库事务的统一管理，确保数据一致性。

**核心要求**:
- 实现事务的开始、提交、回滚机制
- 支持声明式事务管理（可选）
- 确保连接在同一事务中复用
- 处理事务超时与死锁情况

**技术要点**:
- 使用 `ThreadLocal` 绑定事务与线程
- 管理 `Connection` 的 `autoCommit` 状态
- 实现事务传播行为（REQUIRED、REQUIRES_NEW 等）
- 在异常发生时自动触发回滚

**实现方式**:

1. **手动事务管理**
2. **函数式事务管理**
3. **声明式事务管理（推荐）**
---

## 📝 实施建议

### 优先级排序
1. **第一阶段**: 数据库连接池 + CRUD 封装 + 日志处理（必做）
2. **第二阶段**: 统一异常处理 + 事务管理（进阶）
3. **第三阶段**: IoC 容器（进阶）

### 测试验证
- 每个模块完成后编写单元测试
- 使用控制台输出快速验证核心逻辑
- 模拟异常场景测试容错能力

### 代码规范
- 遵循 Java 命名规范（驼峰命名）
- 所有公共方法必须添加注释
- 保持代码简洁，单一职责原则

---

## 🛠️ 技术栈

- **后端框架**: Java Servlet
- **数据库**: MySQL
- **缓存**: Redis
- **文件存储**: 阿里云 OSS
- **构建工具**: Maven
- **日志系统**: java.util.logging (JDK 自带)

---

## 📚 参考资料

- [JDBC 官方文档](https://docs.oracle.com/javase/tutorial/jdbc/)
- [Java Logging API](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html)
- [设计模式 - 工厂模式与单例模式](https://refactoring.guru/design-patterns)
- [ORM 框架原理](https://mybatis.org/mybatis-3/)


