## Why

重新发布已存在的影像数据集时，GeoServer 返回 "unable to remove existing file" 错误。这是因为在创建新的 coverage store 前没有删除已存在的旧 store，旧文件被 GeoServer 内部锁定。

## What Changes

1. **修改 ImageServiceImpl.publishImageDataset() 方法**
   - 在创建 coverage store 之前先调用 deleteStore() 删除已存在的旧 store
   - 等待适当时间让 GeoServer 清理完成
   - 然后再创建新的 coverage store

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` — 修改 publishImageDataset() 方法

## Non-goals

- 不修改影像上传流程的其他部分
- 不修改 GeoServer 配置
- 不修改数据库表结构

## Affected Files

- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
