# 诊断报告: 切片完成后状态被异常重置

## 问题描述

**症状:** 切片任务完成后，状态被错误重置为 `processing/0/idle`  
**观察:** TileSeedService 正确更新了 `completed/seeded` 状态，但随后又有 UPDATE 操作将其覆盖

## 根因分析

### 代码执行流程

```
TileSeedService.triggerSeed()                       TileSeedService.pollSeedStatus()
┌─────────────────────────────────────────┐         ┌─────────────────────────────────────────┐
│ Line 32: getById() → dataset 对象        │         │ Line 77: getById() → ds 对象 (NEW!)    │
│ (status = old value)                     │         │ (独立的对象，不同的内存引用)              │
└─────────────────────────────────────────┘         └─────────────────────────────────────────┘
         │                                                   │
         ▼                                                   │
┌─────────────────────────────┐                            │
│ Line 40-41:                 │                            │
│ dataset.tileStatus = "processing" │                     │
│ dataset.tileProgress = 0    │                            │
└─────────────────────────────┘                            │
         │                                                   │
         ▼                                                   │
┌─────────────────────────────┐                            │
│ Line 42:                    │                            │
│ updateById(dataset)         │                            │
│ → DB: processing/0          │                            │
└─────────────────────────────┘                            │
         │                                                   │
         ▼                                                   │
┌─────────────────────────────┐                            │
│ Line 51:                    │                            │
│ pollSeedStatus() 调用       │ ──────────────────────────▶│ Line 79-81: ds.progress updated
         │                     │                            │ Line 83-88: if progress>=100
         │                     │                            │   ds.tileStatus = "completed"
         │                     │                            │   ds.cacheSeedStatus = "seeded"
         │                     │                            │   updateById(ds) → DB: completed/seeded ✓
         │                     │                            │   break
         │                     │                            │
         │                     │                            │
         │ (pollSeedStatus 返回)│◀──────────────────────────┘
         │                                                   │
         ▼                                                   │
┌─────────────────────────────┐  ← 【BUG!】                  │
│ Line 62:                    │                             │
│ updateById(dataset)         │                             │
│ → DB: processing/0 覆盖!    │                             │
└─────────────────────────────┘                             │
         │                                                   │
         ▼                                                   │
┌─────────────────────────────┐
│ 方法结束                    │
│ 最终 DB 状态: processing/0  │
└─────────────────────────────┘
```

### 问题根因

**Line 62 的 `datasetService.updateById(dataset)` 是罪魁祸首！**

`triggerSeed()` 在 Line 32 获取的 `dataset` 对象，在 Line 40-41 被修改为 `processing/0`，然后在 Line 42 持久化到数据库。

但 `pollSeedStatus()` 在 Line 77 获取的是**另一个独立的 `ds` 对象**（通过另一次 `getById()` 调用）。`pollSeedStatus` 更新的是 `ds` 对象，而不是 `triggerSeed` 持有的 `dataset` 对象。

当 `pollSeedStatus` 完成后，`triggerSeed` 的 `dataset` 对象仍然是 `processing/0` 状态。Line 62 的 `updateById(dataset)` 把这个**过时的状态**又写回数据库，覆盖了 `pollSeedStatus` 刚刚设置的正确状态。

### 证据

1. **注释与实现不符** (Line 53):
   ```java
   // Status already updated by pollSeedStatus (completed or failed)
   ```
   这个注释是**错误的**。`pollSeedStatus` 更新的是它自己持有的 `ds` 对象，不是 `triggerSeed` 的 `dataset` 对象。

2. **两个不同的对象引用**:
   - `triggerSeed` 使用: `dataset` (Line 32 获取)
   - `pollSeedStatus` 使用: `ds` (Line 77 获取)

3. **执行时序**:
   - Line 42: `updateById(dataset)` → processing/0 ✓
   - Line 87: `updateById(ds)` → completed/seeded ✓
   - Line 62: `updateById(dataset)` → processing/0 **覆盖!**

## 修复方案

### 方案: 删除 Line 62 的最终 updateById 调用

**理由:**
- 如果 `pollSeedStatus` 成功完成，它已经在 Line 87 更新了状态
- 如果 `pollSeedStatus` 失败并抛出异常，catch 块 (Line 55-60) 会处理
- Line 62 的调用是**冗余的**，而且会导致 bug

**修改:**
```java
// try 块内，pollSeedStatus 调用后
// 删除这段代码:
// // Status already updated by pollSeedStatus (completed or failed)

// catch 块保持不变
} catch (Exception e) {
    log.error("Tile seed failed for dataset {}", datasetId, e);
    dataset.setTileStatus("failed");
    dataset.setCacheSeedStatus("idle");
    dataset.setTileProgress(0);
}

// 删除 Line 62: datasetService.updateById(dataset);
```

## 相关代码位置

| 文件 | 行号 | 说明 |
|------|------|------|
| `TileSeedService.java` | 32 | `triggerSeed` 获取 dataset 对象 |
| `TileSeedService.java` | 40-41 | 设置 `dataset` 为 processing/0 |
| `TileSeedService.java` | 42 | 第一次 updateById (正确) |
| `TileSeedService.java` | 51 | 调用 pollSeedStatus |
| `TileSeedService.java` | 53 | 错误注释 |
| `TileSeedService.java` | 55-60 | catch 块处理失败 |
| `TileSeedService.java` | 62 | **BUG: 过时的 updateById** |
| `TileSeedService.java` | 77 | `pollSeedStatus` 获取独立的 ds 对象 |
| `TileSeedService.java` | 83-88 | pollSeedStatus 正确更新状态 |

## 验证步骤

1. 触发切片任务
2. 观察日志: "Tile seed completed for dataset X (progress: 100%)"
3. 检查数据库: `tile_status` 应为 `completed`，`cache_seed_status` 应为 `seeded`
4. 确认没有后续的 UPDATE 将状态改回 processing/0
