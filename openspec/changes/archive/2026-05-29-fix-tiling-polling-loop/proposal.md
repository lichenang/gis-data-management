# fix-tiling-polling-loop

## Why

`TileSeedService.pollSeedStatus()` 方法存在轮询不停止的问题：

1. **进度 100% 但状态未更新** - 当 GeoWebCache 返回进度 100% 时，如果 seedStatus 不是 FINISHED/DONE/SUCCEEDED，循环不会退出
2. **状态可能不匹配** - GWC 返回的状态字符串可能与代码检查的值不完全一致，导致提前退出条件不满足

## What Changes

修改 `TileSeedService.pollSeedStatus()` 方法：

1. **增加进度 100% 时自动完成** - 当 `progress >= 100` 时，无论 seedStatus 是什么，都更新状态为 completed 并退出循环
2. **保持现有状态检查** - 仍然保留 FINISHED/DONE/SUCCEEDED/FAILED/ERROR 的检查作为后备

## Capabilities

### Fixed Capabilities
- 切片完成后轮询立即停止
- 状态实时更新为 completed

## Impact

- 修改文件：
  - `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`

## Non-goals

- 不修改前端逻辑
- 不修改其他服务类
