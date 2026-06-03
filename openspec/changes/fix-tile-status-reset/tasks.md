## 1. 后端 - 删除冗余的状态更新调用

- [x] 1.1 修改 `TileSeedService.triggerSeed()`
  - 删除 Line 62 的 `datasetService.updateById(dataset);` 调用
  - 删除 Line 53 的误导性注释
  - 将 updateById 移入 catch 块（仅异常时更新失败状态）

## 2. 验证

- [ ] 2.1 触发切片任务，确认数据库状态正确保持 `completed/seeded`
- [ ] 2.2 观察日志，确认没有多余的 UPDATE 语句覆盖状态
