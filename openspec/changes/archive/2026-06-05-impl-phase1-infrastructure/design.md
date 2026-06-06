## Context

当前基础设施现状：
- `VectorFileFormat`: 已定义 8 种格式（GeoJSON、Shapefile、KML、GML、GPX、CSV、WKT、TopoJSON）
- `FormatDetector`: 已有基础检测逻辑，支持 ZIP 包内容检测
- `VectorDataStoreFactory`: 仅实现 ZIP 解压，未创建真正的 GeoTools DataStore

**问题**：
1. VectorFileFormat 缺少 GeoTools 支持标志
2. FormatDetector 对某些格式（如 KML/KMZ、GML）检测不完整
3. VectorDataStoreFactory 未实现 `createDataStore()` 方法

## Goals / Non-Goals

**Goals:**
1. 扩展 VectorFileFormat 枚举，添加 `geoToolsSupported` 标志
2. 增强 FormatDetector，完善格式检测覆盖
3. 实现 VectorDataStoreFactory.createDataStore() 使用 GeoTools DataStore

**Non-Goals:**
- 不实现完整的多格式导入
- 不修改现有已工作的 GeoJSON 解析

## Decisions

### Decision 1: VectorFileFormat 增加 GeoTools 支持标志

**选项**:
- 添加 `isGeoToolsSupported` boolean 字段
- 添加 `isImportSupported` / `isExportSupported` 分别标志

**选择**: 添加 `isGeoToolsSupported` 字段，标识该格式是否可通过 GeoTools DataStore 直接读取

### Decision 2: VectorDataStoreFactory DataStore 创建方式

**选项**:
- 使用 `DataStoreFinder.getDataStore(Map params)` 通用方式
- 为每种格式创建专用 DataStore

**选择**: 使用 `DataStoreFinder.getDataStore(Map params)`，params 根据格式不同包含 URL 或 InputStream

### Decision 3: Shapefile ZIP 处理

保持现有的 VectorDataStoreFactory.extractToTempDir() 逻辑，解压后使用 ShapefileDataStore 读取 .shp 文件

## Risks / Trade-offs

- GeoTools 32.x 包结构变更可能导致 API 变化 → 使用 org.geotools.api.* 标准接口
- Java 17 模块系统限制 → 在 pom.xml 中配置 release 17 和 JVM args

## Migration Plan

1. 修改 pom.xml 添加缺失的 GeoTools 依赖（gt-shapefile、gt-geojson 等）
2. 扩展 VectorFileFormat 枚举
3. 增强 FormatDetector
4. 实现 VectorDataStoreFactory.createDataStore()
5. 单元测试验证 DataStore 创建
6. 集成测试验证 Shapefile 导入流程
