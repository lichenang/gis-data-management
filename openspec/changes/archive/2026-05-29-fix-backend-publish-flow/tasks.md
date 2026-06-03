## 1. 后端 - 添加 GWC 缓存层删除方法

- [x] 1.1 在 `GeoServerCacheService.java` 添加 `deleteLayer(workspace, layerName)` 方法
  - 调用 `client.delete("/gwc/rest/layers/" + workspace + ":" + layerName)`
  - 捕获异常但只 log warn，不抛出让主流程中断

## 2. 后端 - 调用 GWC 缓存层删除

- [x] 2.1 修改 `ImageServiceImpl.unpublishImageDataset()`
  - 在 `layerService.unpublishLayer()` 后调用 `cacheService.deleteLayer(workspace, layerName)`

## 3. 后端 - 修复切片状态更新时机

- [x] 3.1 修改 `TileSeedService.pollSeedStatus()`
  - 在检测到 `FINISHED/DONE/SUCCEEDED` 时立即更新数据库: `tileStatus="completed"`, `tileProgress=100`, `cacheSeedStatus="seeded"`
  - 新增处理 `FAILED/ERROR` 状态: 立即更新 `tileStatus="failed"`, `cacheSeedStatus="idle"`, `tileProgress=0`

- [x] 3.2 修改 `TileSeedService.triggerSeed()`
  - 移除循环结束后的 `dataset.setTileStatus("completed")` 等状态更新（已由 pollSeedStatus 内部处理）
  - 只在 catch 块中处理异常情况

## 4. 验证

- [ ] 4.1 取消发布影像后，确认 GWC REST API 返回 404（缓存层已删除）
- [ ] 4.2 重新发布同名影像，确认新图层正常关联 GWC，切片状态正常
- [ ] 4.3 触发切片，确认 `tile_status` 在切片完成时立即更新为 `completed`
- [ ] 4.4 模拟切片失败，确认 `tile_status` 立即更新为 `failed`
