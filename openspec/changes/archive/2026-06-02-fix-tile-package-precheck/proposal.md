## Why

当前 `totalWritten == 0` 空瓦片检查位于 `ZipOutputStream` try-with-resources 块内部，抛出异常时 `ZipOutputStream.close()` 已被尝试写入空 ZIP 中央目录（约 22 字节），导致 response 进入"已部分提交"状态。此时 Controller 的 `writeErrorResponse()` 方法即使调用 `response.reset()` 也可能无效（部分 Servlet 容器在已提交后禁止 reset），最终返回损坏的空 ZIP 或 0B 文件。预检查模式将空文件判断提前到打开输出流之前，确保异常在 response 未写入任何数据时抛出，让全局异常处理器能正常返回 JSON 错误。

## What Changes

1. **TilePackageServiceImpl.packageTiles()**：在创建 `ZipOutputStream` 之前添加瓦片文件预检查方法 `precheckTileFiles()`，遍历 GWC 目录统计是否存在瓦片文件。若数量为 0，直接抛出 RuntimeException，不打开输出流
2. **移除 `totalWritten == 0` 后置检查**：删除 try-with-resources 块内部的 `throw new RuntimeException(...)` 空检查，因为预检查已在打开 ZIP 之前拦截空情况
3. **TilePackageController**：移除 catch 块中的 `writeErrorResponse()` 调用，改为不捕获异常，让异常抛给全局异常处理器统一处理
4. **删除 `writeErrorResponse()` 和 `escapeJson()` 方法**（不再需要，由全局处理器接管）

## Capabilities

### Modified Capabilities
- `tile-package-download`: 修改错误处理策略 - 空瓦片检查提前到输出流打开之前，Controller 不再自行处理异常，统一交给 GlobalExceptionHandler

### New Capabilities
（无 — 纯 Bug 修复 + 重构）

## Non-goals

- 不改动瓦片坐标计算逻辑（CRS 转换、tileY 公式）
- 不改变前端下载交互
- 不添加新 API 端点
- 不修改 GlobalExceptionHandler

## Impact

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `backend/.../service/impl/TilePackageServiceImpl.java` | 修改 | 新增预检查方法，删除后置空检查 |
| `backend/.../controller/TilePackageController.java` | 修改 | 移除 catch 块和 writeErrorResponse/escapeJson 方法 |
