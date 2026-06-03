# fix-backend-publish-flow

## Why

影像发布流程存在两个关键后端缺陷：

1. **取消发布导致 GWC 缓存层残留** - 取消发布时只删除 WMS图层，未清理 GeoWebCache 缓存，导致重新发布失败
2. **切片完成后状态更新滞后** - pollSeedStatus 内部循环结束后才更新状态，中途超时或异常时状态卡住

## What Changes

### 缺陷 1: GWC 缓存层清理

在 `ImageServiceImpl.unpublishImageDataset()` 中增加 GWC 缓存层删除：
- 调用 `cacheService.deleteLayer(workspace, layerName)` 删除 GWC 缓存
- GWC REST API: `DELETE /gwc/rest/layers/{workspace}:{layer}`

### 缺陷 2: 切片状态及时更新

优化 `TileSeedService.pollSeedStatus()` 逻辑：
- 在检测到 FINISHED/DONE/SUCCEEDED 时立即更新数据库状态
- 在检测到 FAILED 时也立即更新失败状态
- 不再依赖 poll 循环结束后的统一更新

## Capabilities

### Fixed Capabilities
- 取消发布时完整清理 GWC 缓存层
- 切片状态实时准确更新

## Impact

- 修改文件：
  - `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`
  - `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
  - `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`

## Non-goals

- 不修改前端逻辑
- 不修改切片触发方式
