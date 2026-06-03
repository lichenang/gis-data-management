# fix-hard-unpublish

## Why

之前的取消发布实现不完整，只删除了 WMS 图层，未清理 GeoServer 内部的其他组件：

1. **GWC 缓存层残留** - `DELETE /gwc/rest/layers/{workspace}:{layer}` 未被调用
2. **Coverage Store 未清理** - `DELETE /rest/workspaces/{ws}/coveragestores/{store}?recurse=true` 未被调用
3. **清理顺序错误** - 应该先删 GWC，再删 coverage store，最后更新数据库

导致重新发布时出现图层关联失败、切片状态异常等问题。

## What Changes

重新设计 `unpublishImageDataset()` 取消发布流程，严格按顺序执行：

1. **删除 GWC 缓存层** - `DELETE /gwc/rest/layers/{workspace}:{layer}`
   - 忽略 404 错误（缓存层可能已不存在）
2. **删除 Coverage Store** - `DELETE /rest/workspaces/{ws}/coveragestores/{store}?recurse=true`
   - 级联删除所有关联图层
3. **更新数据库状态** - 设置 status="draft", tileStatus="pending"

## Capabilities

### Fixed Capabilities
- 影像取消发布时完整清理 GeoServer 所有相关组件
- 重新发布不再因残留数据导致失败

## Impact

- 修改文件：
  - `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`
  - `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerLayerService.java`
  - `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

## Non-goals

- 不修改前端逻辑
- 不修改切片触发流程
