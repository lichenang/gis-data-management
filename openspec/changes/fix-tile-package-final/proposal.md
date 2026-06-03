## Why

切片包下载功能返回"该影像尚未生成切片缓存"错误，但 GWC 磁盘目录 `gisplatform_raster_38` 确实存在并包含瓦片文件。根本原因有两个：一是 `tileY()` 使用了 EPSG:4326 线性公式，但 GWC 默认对 ImageMosaic 使用 EPSG:900913 (Web Mercator) 网格集，导致计算的瓦片坐标与实际不匹配；二是 GlobalExceptionHandler 在异常时未重置已设置的 `Content-Type: application/zip`，导致 JSON 错误响应写入失败。

## What Changes

1. **TilePackageServiceImpl.tileY()**：恢复 Web Mercator 公式 `(1 - log(tan(lat) + 1/cos(lat)) / π) / 2 * 2^z`，匹配 GWC EPSG:900913 网格集
2. **GlobalExceptionHandler**：在 `handleRuntimeException` 和 `handleException` 中添加 `response.reset()` 和 `setContentType("application/json;charset=UTF-8")`，确保错误响应不被 Content-Type 冲突影响

## Capabilities

### Modified Capabilities
- `tile-package-download`：修复瓦片 Y 坐标计算公式以匹配 GWC 默认网格集（EPSG:900913），修复异常响应 Content-Type 冲突

## Non-goals

- 不修改 GWC 目录路径拼接逻辑（当前已使用 `workspace + "_" + layerName`）
- 不修改 GeoServer seed 相关逻辑

## Impact

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `backend/.../service/impl/TilePackageServiceImpl.java` | 修改 | tileY() 恢复 Web Mercator 公式 |
| `backend/.../common/GlobalExceptionHandler.java` | 修改 | handleRuntimeException/handleException 添加 response.reset() |
