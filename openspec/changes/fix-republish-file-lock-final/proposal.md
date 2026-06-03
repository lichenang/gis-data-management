## Why

之前的修复（仅删除 coverage store + 等待 2 秒）在某些情况下仍然失败，因为 GeoServer 不仅在数据库中存储配置，还会在文件系统中创建数据目录。即使删除了 REST API 中的 coverage store，文件系统中的数据文件仍可能被 GeoServer 进程持有文件句柄。需要同时清理 GeoServer 数据目录中的对应子目录，确保文件彻底释放。

## What Changes

1. 修改 ImageServiceImpl.publishImageDataset() 方法，在删除 coverage store 后，清理 GeoServer data_dir 中对应的数据子目录
2. 清理完成后等待 2 秒确保文件释放

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` — 修改 publishImageDataset() 方法

## Non-goals

- 不修改其他业务流程
- 不修改数据库表结构
- 不修改 GeoServer REST API 的其他配置
