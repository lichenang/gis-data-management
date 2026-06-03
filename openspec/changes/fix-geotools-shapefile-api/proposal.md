## Why

在 GeoTools 32.x 升级后，Shapefile 导出功能的类名和包路径发生了变化：`org.geotools.shapefile.*` 迁移到 `org.geotools.data.shapefile.*`，数据访问接口从 `org.geotools.data.*`/`org.opengis.feature.simple.*` 迁移到 `org.geotools.api.*`。同时 `ShapefileDataStore` 不再直接暴露 `setTransaction()`/`addFeatures()` 方法，需要通过 `getFeatureSource()` 返回的 `SimpleFeatureStore` 进行写入。当前代码未适配这些变化，导致编译失败。

## What Changes

1. **依赖补充**：在 `pom.xml` 中添加 `gt-shapefile` 模块依赖（已添加但需确认 `dependencyManagement` 和 `dependencies` 两个区域均配置正确）
2. **Import 路径修复**：将所有 Shapefile 相关类的 import 从 `org.geotools.shapefile.*` 修正为 `org.geotools.data.shapefile.*`
3. **API 调用适配**：用 `getFeatureSource().addFeatures()` 替代已移除的 `setTransaction()`/`addFeatures()` 方法；接口引用使用 `org.geotools.api.*` 包路径
4. **诊断文档入库**：将诊断结论提交到项目文档 `openspec/specs/fix-geotools-shapefile-imports.md`

## 非目标

- 不修改其他 GeoTools 模块的导入和用法（如 CrsTransformUtil、GeoTiffParser 中已有的正确引用不动）
- 不涉及 GeoTools 版本升降级（保持 32.0）
- 不重构 Shapefile 导出的整体逻辑
- 不修改前端代码

## Capabilities

### New Capabilities
- `fix-geotools-shapefile-api`: GeoTools 32.x Shapefile 包路径迁移与 API 适配

### Modified Capabilities

无

## Impact

- `openspec/specs/fix-geotools-shapefile-imports.md`：诊断报告（已存在，本次归档入 change）
- `backend/pom.xml`：确认 gt-shapefile 依赖声明
- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`：修正 import 和 Shapefile 写入 API
- `openspec/changes/fix-geotools-shapefile-api/`：change 新增 proposal/design/specs/tasks
