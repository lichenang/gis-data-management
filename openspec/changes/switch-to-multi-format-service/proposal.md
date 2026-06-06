## Why

当前 `DatasetServiceImpl` 使用 `GisDataParserService` 处理数据集解析和导入，但该服务不支持 ZIP 包内的 Shapefile 检测（诊断报告：`openspec/specs/debug-zip-rejection-v2/spec.md`）。系统需要统一的、多格式支持的数据导入服务。

## What Changes

- 创建新的 `MultiFormatImportService` 服务类
- 修改 `DatasetServiceImpl.parseUploadFile()` 方法，改用 `MultiFormatImportService`
- 修改 `DatasetServiceImpl.importDataset()` 方法，改用 `MultiFormatImportService`
- 保留 `GisDataParserService` 接口和实现（暂不删除，避免影响其他依赖）

## Capabilities

### New Capabilities
- `multi-format-import`: 统一的多格式数据导入服务，支持 FormatDetector 深度检测

### Modified Capabilities
<!-- 空：当前 GisDataParserService 的功能将被新服务替代，但这是实现层面的替换 -->

## Impact

**受影响文件**:
- `backend/src/main/java/com/gisplatform/service/MultiFormatImportService.java`（新建）
- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

**依赖关系变化**:
- `DatasetServiceImpl` 增加对 `MultiFormatImportService` 的依赖
- `MultiFormatImportService` 内部组合 `FormatDetector` 和 `VectorDataStoreFactory`

## Non-goals

- 不删除 `GisDataParserService` 接口和实现（保持向后兼容）
- 不修改 Controller 层接口
- 不修改已存在的单元测试
