## Why

DatasetController 和 ImageController 的 delete 方法手动构造 UPDATE 语句，但未设置 `deleted=1`，导致数据库记录未被逻辑删除，deleted 字段仍为 0。MyBatis-Plus 的 `@TableLogic` 注解需要配合 `removeById` 或 `deleteById` 方法使用才能自动设置 deleted 字段。

## What Changes

1. 修改 DatasetController 的 delete 方法，使用 MyBatis-Plus 的 `removeById` 替代手动的 UPDATE 语句
2. 修改 ImageController 的 delete 方法，同样使用 `removeById`
3. 确保 Dataset 和 Image 实体的 deleted 字段配置了 @TableLogic 注解

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/controller/DatasetController.java` — 修改 delete 方法
- `backend/src/main/java/com/gisplatform/controller/ImageController.java` — 修改 delete 方法

## Non-goals

- 不修改查询逻辑
- 不修改其他接口
