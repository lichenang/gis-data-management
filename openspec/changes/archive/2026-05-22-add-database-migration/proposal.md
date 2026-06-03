## Why

当前项目缺少数据库初始化脚本，无法启动应用。需要使用 Flyway 实现数据库版本管理，创建核心表结构和初始数据，为应用提供可用的数据库环境。

## What Changes

- 添加 Flyway 依赖到 pom.xml
- 创建 V1__init_schema.sql 包含所有核心表 DDL
- 创建 V2__init_data.sql 包含初始用户、角色、权限数据
- 配置 application.yml 中 Flyway 相关参数
- 创建示例矢量要素表（vector_features_001）及其 GiST 索引

## Capabilities

### New Capabilities
- `database-migration`: 使用 Flyway 管理数据库版本，实现表结构初始化和初始数据导入

### Modified Capabilities
- 无

## Impact

- 新增文件：
  - `backend/src/main/resources/db/migration/V1__init_schema.sql`
  - `backend/src/main/resources/db/migration/V2__init_data.sql`
- 修改文件：
  - `backend/pom.xml`（添加 flyway-core 和 flyway-database-postgresql 依赖）
  - `backend/src/main/resources/application.yml`（添加 Flyway 配置）

## Non-goals

- 不实现数据集表动态创建功能（后续迭代）
- 不实现数据迁移回滚功能
- 不实现完整的数据备份恢复功能
