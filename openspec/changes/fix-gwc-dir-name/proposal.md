## Why

`TilePackageServiceImpl` 在构造 GWC 缓存目录路径时使用 `raster_38`（即 `layerName`），但 GWC 实际的磁盘目录名为 `gisplatform_raster_38`（包含 workspace 前缀），导致"切片缓存目录不存在"错误。该 Bug 使所有影像的切片包下载功能完全失效。

## What Changes

1. **TilePackageServiceImpl**：修正 GWC 目录路径构造逻辑，将 `layerName` 改为 `workspace + "_" + layerName`（如 `gisplatform_raster_38`）
2. **GlobalExceptionHandler**：确保异常响应时不会因 response headers 已设置而失败（response reset 后正确写入 JSON）

## Capabilities

### Modified Capabilities
- `tile-package-download`：修复 GWC 目录路径拼接错误，确保瓦片包下载能正确定位 GWC 缓存目录

## Non-goals

- 不修改 GWC seed 相关逻辑
- 不修改 GlobalExceptionHandler 的核心异常处理逻辑（仅确保其健壮性）
- 不添加新 API 端点

## Impact

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `backend/.../service/impl/TilePackageServiceImpl.java` | 修改 | GWC 目录路径从 `{dataDir}/gwc/{layerName}` 改为 `{dataDir}/gwc/{workspace}_{layerName}` |
