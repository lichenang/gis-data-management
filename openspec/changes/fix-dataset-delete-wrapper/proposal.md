## Why

MyBatis-Plus 的 `updateById` 方法在实体字段标注了 `@TableLogic` 后，会自动忽略该字段（在非删除场景下）。这导致在 Dataset 或 Image 实体上设置 `deleted=1` 后调用 `updateById`，生成的 SQL 中不包含 `deleted` 列，导致逻辑删除无法执行。

## What Changes

1. 修改 DatasetService.deleteDataset() 方法，使用 `LambdaUpdateWrapper` 显式设置 `deleted=1`
2. 修改 ImageService.deleteImage() 方法，同样使用 `LambdaUpdateWrapper`
3. 仅更新 `deleted` 和 `update_time` 字段

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
- 不修改其他接口
