# 设计: fix-tiling-polling-loop

## 问题分析

### 当前问题

`pollSeedStatus` 方法的退出条件：

```java
if ("FINISHED".equals(seedStatus) || "DONE".equals(seedStatus) || "SUCCEEDED".equals(seedStatus)) {
    // 更新状态并 break
}

// 缺少: progress == 100 时自动完成的逻辑
```

当 GWC 返回 `progress = 100` 但 `seedStatus` 不是预期值时，循环继续。

### 修改方案

在检查 seedStatus 之前，先检查进度：

```java
if (status != null) {
    Integer progress = (Integer) status.get("progress");
    String seedStatus = (String) status.get("status");

    Dataset ds = datasetService.getById(datasetId);
    if (ds != null) {
        ds.setTileProgress(progress != null ? progress : 0);

        // 新增: 进度100%时自动完成
        if (progress != null && progress >= 100) {
            log.info("Tile seed completed for dataset {} (progress: {}%)", datasetId, progress);
            ds.setTileStatus("completed");
            ds.setCacheSeedStatus("seeded");
            datasetService.updateById(ds);
            break;
        }

        ds.setCacheSeedStatus("seeding");
        datasetService.updateById(ds);

        // 原有的状态检查保留
        if ("FINISHED".equals(seedStatus) || "DONE".equals(seedStatus) || "SUCCEEDED".equals(seedStatus)) {
            log.info("Seed task finished for dataset {}", datasetId);
            ds.setTileStatus("completed");
            ds.setCacheSeedStatus("seeded");
            datasetService.updateById(ds);
            break;
        }

        if ("FAILED".equals(seedStatus) || "ERROR".equals(seedStatus)) {
            log.error("Seed task failed for dataset {}", datasetId);
            ds.setTileStatus("failed");
            ds.setCacheSeedStatus("idle");
            ds.setTileProgress(0);
            datasetService.updateById(ds);
            break;
        }
    }
}
```

## 验证步骤

1. 触发切片任务
2. 观察数据库 `tile_status` 在进度达到 100% 时立即变为 `completed`
3. 确认轮询循环在完成后立即停止（不再继续请求）
