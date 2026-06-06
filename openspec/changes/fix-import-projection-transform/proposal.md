## Why

当前 Shapefile 导入时，`MultiFormatImportServiceImpl.importUsingDataStore()` 直接将 GeoTools DataStore 返回的原始坐标写入 PostGIS，未进行投影转换。用户导入非 EPSG:4326 坐标系的 Shapefile（如 EPSG:3857 Web Mercator）后，GeoJSON 接口返回的坐标是米制投影值（如 38453552.737），导致前端地图定位错误。

## What Changes

- 在 `MultiFormatImportServiceImpl.importUsingDataStore()` 中添加投影转换逻辑
- 从 GeoTools DataStore 获取 Shapefile 的原生 CRS
- 创建从原生 CRS 到 EPSG:4326 的 MathTransform
- 遍历所有 Feature，对其 Geometry 执行 JTS.transform() 转换
- 将转换后的 Geometry 写入 PostGIS

## Capabilities

### New Capabilities
- `import-projection-transform`: Shapefile 导入时自动投影转换

### Modified Capabilities
<!-- 空：现有导入功能的行为变更（修复 bug） -->

## Impact

**受影响文件**:
- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

**依赖**:
- 项目已有的 `CrsTransformUtil.java`（提供 CRS 转换工具方法）
- GeoTools 32.x 的 `CRS.findMathTransform()` 和 `JTS.transform()`

## Non-goals

- 不修改 GeoJSON 导出逻辑（数据已在库中为 EPSG:4326）
- 不修改其他格式（KML、GML、GPX 等）的导入逻辑
- 不实现从 EPSG:4326 到其他 CRS 的导出转换
