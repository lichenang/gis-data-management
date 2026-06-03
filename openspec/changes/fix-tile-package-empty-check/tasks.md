## 1. 后端空缓存检查

- [x] 1.1 `TilePackageServiceImpl.java` 在 ZIP 写入循环后、`log.info` 之前，添加 `if (totalWritten == 0)` 检查，抛出 `RuntimeException` 并提示用户触发切片种子任务
