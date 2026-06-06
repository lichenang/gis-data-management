## Why

当前 Shapefile 解析使用 GeoTools 的 FileDataStore API，但存在以下问题：
1. Java 17 模块系统下运行时可能出现访问异常（`IllegalAccessError`），需要配置 `--add-opens` / `--add-exports` 参数
2. 现有实现混用 `org.opengis.*` 和 GeoTools API，可能导致运行时兼容性问题

改用官方推荐的 ShapefileDataStore 可获得更好的兼容性和维护性。

## What Changes

- 确认 pom.xml 中 `gt-shapefile 32.0` 依赖已正确配置
- 在 maven-compiler-plugin 中添加 Java 17 模块开放参数（`--add-opens` / `--add-exports`）
- 将 `parseShapefile` 方法重构为基于 `ShapefileDataStore` 的标准实现，统一使用 GeoTools 32.x API

## Capabilities

### New Capabilities
<!-- 不引入新能力，仅为重构修复 -->

### Modified Capabilities
- `shapefile-import`: 修复 Shapefile 导入功能的 Java 17 兼容性问题，统一使用 GeoTools ShapefileDataStore API

## Impact

- **受影响文件**:
  - `backend/pom.xml`: 添加 maven-compiler-plugin 模块开放参数
  - `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportService.java`: 重构 parseShapefile 方法

## Non-goals

- 不实现 Shapefile 导出功能（当前 ExportController 中已标记暂不可用）
- 不修改数据库 schema 或数据模型
- 不引入新的 REST API 端点
