# 设计: fix-hard-unpublish

## 取消发布流程重新设计

### 当前问题

之前的取消发布实现分散且不完整：

```java
// 当前 ImageServiceImpl.unpublishImageDataset() 流程：
layerService.unpublishLayer(workspace, layerName);  // 只删 WMS 图层
// 缺少：GWC 缓存层删除
// 缺少：Coverage store 删除
dataset.setStatus("draft");
```

### 新流程设计

按顺序执行，确保完整清理：

```
┌──────────────────────────────────────────────────────────────┐
│                  unpublishImageDataset()                      │
└──────────────────────────────────────────────────────────────┘
                           │
                           ▼
         ┌─────────────────────────────────────┐
         │  Step 1: 删除 GWC 缓存层             │
         │  cacheService.deleteLayer()         │
         │  DELETE /gwc/rest/layers/{ws}:{layer}
         │  忽略 404 错误                       │
         └─────────────────────────────────────┘
                           │
                           ▼
         ┌─────────────────────────────────────┐
         │  Step 2: 删除 Coverage Store        │
         │  layerService.deleteCoverageStore() │
         │  DELETE /rest/workspaces/{ws}/      │
         │       coveragestores/{store}?recurse=true
         │  级联删除所有图层                    │
         └─────────────────────────────────────┘
                           │
                           ▼
         ┌─────────────────────────────────────┐
         │  Step 3: 更新数据库状态              │
         │  status = "draft"                   │
         │  tileStatus = "pending"             │
         │  cacheSeedStatus = "idle"           │
         └─────────────────────────────────────┘
```

## 修改 1: GeoServerCacheService.deleteLayer

增强错误处理，忽略 404：

```java
public void deleteLayer(String workspace, String layerName) {
    String layerId = workspace + ":" + layerName;
    try {
        client.delete("/gwc/rest/layers/" + layerId, String.class);
        log.info("Deleted GWC layer: {}", layerId);
    } catch (Exception e) {
        // 忽略 404（缓存层可能已不存在）
        if (e.getMessage() != null && e.getMessage().contains("404")) {
            log.info("GWC layer already deleted or not found: {}", layerId);
        } else {
            log.warn("Failed to delete GWC layer: {}", layerId, e);
        }
    }
}
```

## 修改 2: GeoServerLayerService.deleteCoverageStore

新增方法，支持级联删除：

```java
public void deleteCoverageStore(String workspace, String storeName) {
    try {
        client.delete(
            "/rest/workspaces/" + workspace + "/coveragestores/" + storeName + "?recurse=true",
            String.class
        );
        log.info("Deleted coverage store: {} with all layers", storeName);
    } catch (Exception e) {
        log.warn("Failed to delete coverage store: {}", storeName, e);
    }
}
```

## 修改 3: ImageServiceImpl.unpublishImageDataset

重构调用顺序：

```java
@Transactional(rollbackFor = Exception.class)
public Dataset unpublishImageDataset(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("数据集不存在");
    }

    String workspace = geoServerProperties.getWorkspace();
    String layerName = "raster_" + id;
    String storeName = layerName;  // coverage store 与 layer 同名

    // Step 1: Delete GWC cache layer (ignore 404)
    cacheService.deleteLayer(workspace, layerName);

    // Step 2: Delete coverage store with all layers
    layerService.deleteCoverageStore(workspace, storeName);

    // Step 3: Reset status
    dataset.setStatus("draft");
    dataset.setTileStatus("pending");
    dataset.setTileProgress(0);
    dataset.setCacheSeedStatus("idle");
    dataset.setUpdateTime(LocalDateTime.now());

    this.updateById(dataset);

    return dataset;
}
```

## 验证步骤

1. 取消发布影像，通过 GeoServer REST API 确认：
   - GWC 缓存层已删除 (`GET /gwc/rest/layers/{ws}:{layer}` 返回 404)
   - Coverage store 已删除
2. 重新发布同名影像，确认新图层正常关联 GWC
3. 多次取消/发布循环，确认无残留数据
