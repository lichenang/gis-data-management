## 1. 后端 - 增强 GWC 删除方法

- [x] 1.1 修改 `GeoServerCacheService.deleteLayer()`
  - 增加 404 错误处理，忽略缓存层不存在的情况

## 2. 后端 - 添加 Coverage Store 删除方法

- [x] 2.1 在 `GeoServerLayerService.java` 添加 `deleteCoverageStore(workspace, storeName)` 方法
  - 调用 `client.delete("/rest/workspaces/{ws}/coveragestores/{store}?recurse=true")`
  - 捕获异常但只 log warn

## 3. 后端 - 重构取消发布流程

- [x] 3.1 修改 `ImageServiceImpl.unpublishImageDataset()`
  - Step 1: 调用 `cacheService.deleteLayer(workspace, layerName)`
  - Step 2: 调用 `layerService.deleteCoverageStore(workspace, storeName)`
  - Step 3: 更新数据库状态

## 4. 验证

- [ ] 4.1 取消发布后确认 GWC 缓存层返回 404
- [ ] 4.2 取消发布后确认 Coverage store 已删除
- [ ] 4.3 重新发布同名影像，确认正常关联
- [ ] 4.4 多次取消/发布循环，确认无残留
