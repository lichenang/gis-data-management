## Why

当前数据库迁移脚本（V1__init_schema.sql）缺少表和字段的中文注释，不符合项目代码规范要求。规范明确要求所有数据库表和字段必须有中文注释，以便开发人员和数据库管理员理解表结构。

## What Changes

- 创建 Flyway 迁移脚本 V3__add_table_comments.sql
- 为所有 20+ 张数据库表添加 COMMENT ON 语句
- 为所有关键字段添加中文注释
- 检查并补充现有实体类的 @Schema 或 Javadoc 注释

## Capabilities

### New Capabilities
- `database-comments`: 数据库表和字段的中文注释补充

### Modified Capabilities
- 无

## Impact

- 新增文件：`backend/src/main/resources/db/migration/V3__add_table_comments.sql`
- 修改文件：检查并补充实体类注释（如有缺失）

## Non-goals

- 不修改已有的表结构（仅添加注释）
- 不创建新的数据表
- 不修改现有业务逻辑
