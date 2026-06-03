# 设计: fix-tile-status-reset

## 问题详解

### 当前代码执行流程

```
TileSeedService.triggerSeed()                       TileSeedService.pollSeedStatus()
┌─────────────────────────────┐                    ┌─────────────────────────────┐
│ dataset 对象 (Line 32 获取)  │                    │ ds 对象 (Line 77 获取)       │
│ 状态: old → processing/0    │                    │ 独立的对象引用               │
└──────────────┬──────────────┘                    └──────────────┬──────────────┘
               │                                               │
               ▼                                               ▼
      updateById(dataset)                            更新 ds 进度和状态
      (Line 42) → DB: processing/0 ✓                 (Line 79-87)
                                                            │
                                                            ▼
                                                   updateById(ds)
                                                   (Line 87) → DB: completed/seeded ✓
                                                            │
                                                            ◀── pollSeedStatus 返回
               │
               ▼
      updateById(dataset)  ← 【BUG!】
      (Line 62) → DB: processing/0 覆盖!
```

### 修改方案

删除 Line 62 的 `datasetService.updateById(dataset);`

```java
// try 块内
try {
    String layerName = "raster_" + datasetId;
    cacheService.seedLayer(...);
    pollSeedStatus(datasetId, layerName);
    // 删除这段注释和调用:
    // Status already updated by pollSeedStatus (completed or failed)
    // datasetService.updateById(dataset);  ← 删除这行

} catch (Exception e) {
    log.error("Tile seed failed for dataset {}", datasetId, e);
    dataset.setTileStatus("failed");
    dataset.setCacheSeedStatus("idle");
    dataset.setTileProgress(0);
}

// 不再需要 Line 62 的 updateById
```

## 验证步骤

1. 触发切片任务
2. 观察日志: "Tile seed completed for dataset X (progress: 100%)"
3. 检查数据库: `tile_status` 应为 `completed`，`cache_seed_status` 应为 `seeded`
4. 确认没有后续的 UPDATE 将状态改回 processing/0
