## Context

项目使用 Spring Boot 3.5 + MyBatis-Plus + PostgreSQL/PostGIS 技术栈。当前项目缺少数据库初始化方案，需要：
- 启用 Flyway 进行数据库版本管理
- 创建所有核心业务表
- 插入初始数据（管理员用户、基础角色）

## Goals / Non-Goals

**Goals:**
- 添加 Flyway 依赖到 Maven 项目
- 创建完整的数据库表结构（20+ 张表）
- 插入初始数据（admin 用户、3 个角色、权限数据）
- 配置 Flyway 自动执行迁移

**Non-Goals:**
- 实现数据回滚脚本
- 实现增量数据迁移
- 实现备份恢复功能

## Decisions

**1. Flyway 依赖选择：**
- 方案 A：仅 flyway-core
- 方案 B：flyway-core + flyway-database-postgresql

**决定采用方案 B**：Spring Boot 3.x 需要额外添加数据库特定驱动，flyway-database-postgresql 提供更好的 PostgreSQL 支持。

**2. 迁移脚本拆分策略：**
- V1__init_schema.sql：所有 DDL（CREATE TABLE、CREATE INDEX）
- V2__init_data.sql：初始数据（INSERT）

**决定采用分离策略**：便于问题定位和未来扩展。

**3. 表命名风格：**
- 用户/角色表：`sys_user`, `sys_role`, `sys_permission`
- 业务表：`dataset`, `raster_metadata`, `map_layer`
- 索引命名：`idx_<表名>_<列名>`

## Risks / Trade-offs

- [风险] Flyway 与 MyBatis-Plus 共存时可能冲突 → 已验证 MyBatis-Plus 使用 DDL auto=.NONE，不会影响 Flyway
- [风险] 首次启动需要空数据库 → 文档说明需要手动创建空数据库 `gisdb`
- [风险] PostGIS 扩展未安装 → Flyway 脚本第一步自动创建扩展

## Migration Plan

1. 在 pom.xml 添加 Flyway 依赖
2. 创建 V1__init_schema.sql
3. 创建 V2__init_data.sql
4. 配置 application.yml 中的 Flyway 参数
5. 启动应用自动执行迁移
6. 验证表结构和数据

## Open Questions

- 是否需要创建示例矢量数据表？（决定：创建一张示例表 vector_features_001）
- 初始管理员密码是否固定为 admin123？（决定：固定，便于测试，文档中说明生产环境需修改）
