## Context

add-database-migration 创建的 V1__init_schema.sql 缺少 COMMENT 语句。数据库表和字段的中文注释对于：
- 团队协作理解表结构
- 数据库文档生成
- 排查问题时的快速理解

至关重要。

## Goals / Non-Goals

**Goals:**
- 创建 V3__add_table_comments.sql 迁移脚本
- 为所有表添加中文 COMMENT
- 为关键字段添加中文 COMMENT
- 检查实体类注释完整性

**Non-Goals:**
- 不修改表结构
- 不添加新表
- 不修改业务逻辑

## Decisions

**注释内容来源：**
- 参照 openspec/specs/database-init.md 中各表的字段说明
- 实体类的 @Schema 注解说明

**注释语言：** 中文

## Migration Plan

1. 阅读 database-init.md 获取字段说明
2. 检查现有实体类注释
3. 生成 V3__add_table_comments.sql
4. 验证 SQL 语法正确

## Open Questions

- 是否需要同时生成数据库文档？（决定：暂不需要，Flyway 注释已足够）
