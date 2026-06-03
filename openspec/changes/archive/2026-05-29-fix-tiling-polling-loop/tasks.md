## 1. 后端 - 修复轮询停止条件

- [x] 1.1 修改 `TileSeedService.pollSeedStatus()`
  - 在检查 seedStatus 之前，先检查 `progress >= 100`
  - 当进度达到 100 时，更新 `tileStatus="completed"` 和 `cacheSeedStatus="seeded"`，然后 break
  - 保留原有的 FINISHED/DONE/SUCCEEDED/FAILED/ERROR 状态检查作为后备

## 2. 验证

- [ ] 2.1 触发切片任务，确认进度 100% 时状态立即变为 completed
- [ ] 2.2 确认轮询在完成后立即停止，不再继续请求 GWC
