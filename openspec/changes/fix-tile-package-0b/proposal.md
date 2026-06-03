## Why

瓦片包下载功能在特定条件下返回 0B 空文件，导致前端下载失败且无明确错误提示。经诊断排查定位到三个独立但共同作用的 Bug：Tile Y 坐标公式与 GWC 实际网格集不匹配、Extent CRS 未转换导致瓦片范围全越界、Controller 异常处理路径混用 getOutputStream/getWriter 导致错误响应被吞噬。三个 Bug 同时触发生成 0B 响应，无法诊断问题原因，必须优先修复。

## What Changes

1. **TilePackageServiceImpl.tileY()**：将硬编码的 Web Mercator 瓦片 Y 公式改为适配 EPSG:4326 网格集的线性公式（`(90 - lat) / 180 * 2^z`），确保 Y 坐标与 GWC 磁盘目录结构一致
2. **TilePackageServiceImpl.parseBounds()**：从 `dataset.extent` 读取范围后，根据 `dataset.srs` 通过 `CrsTransformUtil.transformExtentToWgs84()` 转换到 EPSG:4326，确保瓦片坐标计算使用正确的经纬度值
3. **TilePackageController**：移除 catch 块中 `response.getWriter()` 调用，改为抛出 `RuntimeException` 由全局异常处理器 `GlobalExceptionHandler` 统一返回 JSON 错误响应，消除 Servlet API 流冲突

## Capabilities

### New Capabilities
- `tile-package-download`: 瓦片包下载功能的正确实现，覆盖范围计算、坐标转换、异常处理三个方面，确保下载完整的瓦片缓存 ZIP 包

### Modified Capabilities

（无 — 纯 Bug 修复，不涉及既有能力规格变更）

## Non-goals

- 不改变瓦片种子生成（seeding）功能
- 不涉及前端下载交互逻辑修改
- 不改动 `GeoServerCacheService` 的 seed API 调用方式
- 不添加新的 API 端点

## Impact

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `backend/.../controller/TilePackageController.java` | 修改 | 异常处理路径中移除 getWriter，改用 throw |
| `backend/.../service/impl/TilePackageServiceImpl.java` | 修改 | tileY 公式改为线性、parseBounds 增加 CRS 转换 |
