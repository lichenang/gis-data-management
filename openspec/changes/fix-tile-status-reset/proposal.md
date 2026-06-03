# fix-tile-status-reset

## Why

`TileSeedService.triggerSeed()` 存在 bug：切片任务完成后，状态被错误重置为 `processing/0`。

**根因:** Line 62 的 `datasetService.updateById(dataset)` 是冗余且有害的调用。`triggerSeed()` 和 `pollSeedStatus()` 各有自己的 `Dataset` 对象引用（不同的内存地址）。`pollSeedStatus()` 正确更新了它持有的 `ds` 对象（状态变为 `completed/seeded`），但 `triggerSeed()` 在 Line 62 用过时的 `dataset` 对象（仍为 `processing/0`）执行 `updateById`，覆盖了正确值。

## What Changes

删除 `TileSeedService.triggerSeed()` Line 62 的 `datasetService.updateById(dataset);` 调用。

**理由:**
- `pollSeedStatus()` 成功完成时已在 Line 87 更新状态
- `pollSeedStatus()` 失败时抛异常，catch 块 (Line 55-60) 处理
- Line 62 的调用是冗余的，且会导致 bug

## Capabilities

### Fixed Capabilities
- 切片完成后状态保持 `completed/seeded`

## Impact

- 修改文件：
  - `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`

## Non-goals

- 不修改其他服务类
- 不修改前端逻辑
