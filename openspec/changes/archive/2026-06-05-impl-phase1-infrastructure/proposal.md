## Why

根据 `openspec/specs/multi-format-vector-support.md` 的设计，Phase 1 聚焦基础设施搭建。当前系统虽已有 `FormatDetector`、`VectorDataStoreFactory` 和 `VectorFileFormat` 的基础实现，但尚未完整集成 GeoTools DataStore，无法支持多格式矢量数据的真正解析与导入。

## What Changes

- 扩展 `VectorFileFormat` 枚举，添加 `isGeoToolsSupported()` 方法
- 增强 `FormatDetector.detect()` 方法，完善格式检测逻辑
- 实现 `VectorDataStoreFactory.createDataStore()` 方法，使用 GeoTools DataStoreFinder 创建各格式 DataStore
- 支持 Shapefile（含 ZIP 包）通过 GeoTools ShapefileDataStore 正确解析
- 添加 GeoTools 32.x 所需的 Maven 依赖（gt-shapefile、gt-geojson 等）

## Capabilities

### Modified Capabilities
- `shapefile-import`: 完善 Shapefile 导入基础设施，使用 GeoTools ShapefileDataStore

## Impact

- **受影响文件**:
  - `backend/pom.xml`: 添加 GeoTools 相关依赖
  - `backend/.../common/enums/VectorFileFormat.java`: 扩展枚举
  - `backend/.../service/FormatDetector.java`: 增强格式检测
  - `backend/.../service/VectorDataStoreFactory.java`: 实现 DataStore 创建

## Non-goals

- 不实现完整的 KML、GML、GPX、CSV 等格式的导入（Phase 2）
- 不实现导出功能（Phase 3）
- 不修改前端界面（Phase 4）
