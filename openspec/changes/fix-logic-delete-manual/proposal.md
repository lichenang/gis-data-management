## Why

手动修复 ImageServiceImpl.deleteImage 和 DatasetServiceImpl.deleteDataset 中的逻辑删除问题。当前代码使用 `updateById(dataset)` 配合 `setDeleted(1)`，但 MyBatis-Plus 会忽略 `@TableLogic` 注解标记的字段，导致 deleted 列不在 SQL 中。

## What Changes

1. 修改 DatasetServiceImpl.deleteDataset() 方法，使用 LambdaUpdateWrapper 显式设置 deleted=1
2. 修改 ImageServiceImpl.deleteImage() 方法，使用 LambdaUpdateWrapper 显式设置 deleted=1

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java` — 修改 deleteDataset 方法
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` — 修改 deleteImage 方法

## Non-goals

- 不修改查询逻辑
- 不修改其他业务逻辑
