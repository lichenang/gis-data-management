# 设计: fix-backend-publish-flow

## 缺陷 1: GWC 缓存层清理

### 当前问题

`ImageServiceImpl.unpublishImageDataset()` 只删除了 WMS 图层：

```java
// ImageServiceImpl.java line 317
layerService.unpublishLayer(workspace, layerName);
// 缺少: cacheService.deleteLayer(workspace, layerName);
```

`GeoServerCacheService` 缺少 `deleteLayer` 方法。

### 修改方案

#### 1. 在 GeoServerCacheService 添加 deleteLayer 方法

```java
public void deleteLayer(String workspace, String layerName) {
    String layerId = workspace + ":" + layerName;
    try {
        client.delete("/gwc/rest/layers/" + layerId, String.class);
        log.info("Deleted GWC layer: {}", layerId);
    } catch (Exception e) {
        log.warn("Failed to delete GWC layer: {}", layerId, e);
    }
}
```

#### 2. 在 ImageServiceImpl.unpublishImageDataset() 调用

```java
// Unpublish layer
layerService.unpublishLayer(workspace, layerName);

// Delete GWC cache layer
cacheService.deleteLayer(workspace, layerName);
```

## 缺陷 2: 切片状态更新

### 当前问题

`TileSeedService.pollSeedStatus()` 检测到完成状态后只 `break`，不更新数据库：

```java
// TileSeedService.java line 87-89
if ("FINISHED".equals(seedStatus) || "DONE".equals(seedStatus) || "SUCCEEDED".equals(seedStatus)) {
    log.info("Seed task finished for dataset {}", datasetId);
    break;  // 只是 break，状态在 triggerSeed 结束时才更新
}
```

当 poll 超时（120次 × 5秒 = 10分钟）或异常退出时，`triggerSeed` 不会设置 completed 状态。

### 修改方案

在 `pollSeedStatus` 检测到终止状态时立即更新数据库：

```java
if ("FINISHED".equals(seedStatus) || "DONE".equals(seedStatus) || "SUCCEEDED".equals(seedStatus)) {
    log.info("Seed task finished for dataset {}", datasetId);
    ds.setTileStatus("completed");
    ds.setTileProgress(100);
    ds.setCacheSeedStatus("seeded");
    datasetService.updateById(ds);
    break;
}

// 新增: 处理失败状态
if ("FAILED".equals(seedStatus) || "ERROR".equals(seedStatus)) {
    log.error("Seed task failed for dataset {}", datasetId);
    ds.setTileStatus("failed");
    ds.setCacheSeedStatus("idle");
    ds.setTileProgress(0);
    datasetService.updateById(ds);
    break;
}
```

同时修改 `triggerSeed` 结尾逻辑，只在非正常退出时更新状态：

```java
// pollSeedStatus 内部已更新状态，这里只处理异常情况
} catch (Exception e) {
    log.error("Tile seed failed for dataset {}", datasetId, e);
    dataset.setTileStatus("failed");
    dataset.setCacheSeedStatus("idle");
    dataset.setTileProgress(0);
    datasetService.updateById(dataset);
}
// 不再在这里设置 completed（由 pollSeedStatus 内部处理）
```

## 验证步骤

1. 取消发布影像后，通过 GeoServer REST API 确认 GWC 缓存层已删除
2. 重新发布同名影像，确认新图层正常关联 GWC
3. 触发切片，观察 `tile_status` 在切片完成时立即更新为 `completed`
4. 模拟切片失败，确认 `tile_status` 立即更新为 `failed`
