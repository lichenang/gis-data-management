## Context

MyBatis-Plus 的逻辑删除功能通过 `@TableLogic` 注解实现。当实体类字段标注此注解后，调用 `deleteById` 等方法时，会自动将 DELETE 语句转换为 UPDATE 语句，设置 deleted 字段为指定值（默认为 1）。

当前问题：删除数据集后，数据库中 deleted 字段仍为 0，说明逻辑删除未生效。

## Goals / Non-Goals

**Goals:**
- 确保 Dataset 实体的 @TableLogic 注解配置正确
- 确保 MyBatis-Plus 全局配置正确
- 验证删除操作后 deleted=1

**Non-goals:**
- 不修改其他表或实体

## Decisions

### Decision 1: 检查 @TableLogic 注解

**选择**: 在 Dataset 实体的 deleted 字段添加 @TableLogic 注解

**理由**:
- @TableLogic 是 MyBatis-Plus 逻辑删除的标准注解
- 缺少此注解会导致执行真正的 DELETE 语句

### Decision 2: 验证全局配置

**选择**: 检查 application.yml 中的逻辑删除配置

**理由**:
- MyBatis-Plus 全局配置可以设置默认值（logic-delete-value: 1）

## Check Points

1. Dataset.java - 检查 deleted 字段是否有 @TableLogic 注解
2. application.yml - 检查 mybatis-plus.global-config.db-config.logic-delete-field
3. 检查 MybatisPlusConfig 是否全局注册了逻辑删除插件

## Implementation Steps

1. 在 Dataset.deleted 字段添加 @TableLogic 注解
2. 确保 application.yml 配置正确
3. 验证删除后 deleted 字段值

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 配置不生效 | 可能有多个配置冲突 | 检查注解优先级高于全局配置 |
