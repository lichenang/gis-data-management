## Why

删除数据集时，DELETE 请求到达后端后执行的 UPDATE 语句没有设置 `deleted=1`，仅更新了 `update_time`，导致数据库中 `deleted` 字段仍为 0（表示未删除）。需要检查并修复逻辑删除配置。

## What Changes

1. 检查 DatasetController 的 delete 方法是否正确使用了 MyBatis-Plus 的逻辑删除
2. 确保 Dataset 实体类的 `@TableLogic` 注解配置正确
3. 验证 MyBatis-Plus 的全局逻辑删除配置（deleted=1 表示已删除）

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/entity/Dataset.java` — 检查 @TableLogic 注解
- `backend/src/main/java/com/gisplatform/controller/DatasetController.java` — 检查 delete 方法

## Non-goals

- 不修改其他业务逻辑
- 不修改数据库表结构
