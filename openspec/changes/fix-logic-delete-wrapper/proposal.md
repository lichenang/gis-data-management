## Why

ImageServiceImpl.deleteImage 和 DatasetServiceImpl.deleteDataset 方法使用 `updateById(dataset)` 手动设置 deleted=1，但 MyBatis-Plus 会忽略标记了 `@TableLogic` 注解的字段，导致生成的 SQL 中不包含 deleted 列，使得逻辑删除未生效。

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
