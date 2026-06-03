## Why

重新发布已存在的影像数据集时，GeoServer 可能仍然持有上次发布的文件句柄（文件锁），导致创建新的 coverage store 时返回 500 错误。需要先删除已有的 store，等待 GeoServer 释放文件句柄后，再创建新的 store。

## What Changes

1. 修改 ImageServiceImpl.publishImageDataset() 方法，在创建 coverage store 前先删除已有的 store
2. 增加 2 秒等待时间（Thread.sleep）确保 GeoServer 释放文件句柄

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
- 不修改 GeoServer 的其他配置
