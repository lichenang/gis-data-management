## Why

当前 Shapefile 格式支持 ZIP 包上传，但 FormatDetector 在检测 ZIP 文件格式时仅识别为 ZIP 类型，无法正确识别其内部包含的 .shp 文件。用户上传包含 Shapefile 的 ZIP 包时，系统无法正确处理该文件。

## What Changes

- 修改 FormatDetector 的格式检测逻辑
- 对于扩展名为 .zip 的文件，深度检查其内部是否包含 .shp 文件
- 若 ZIP 包内包含 .shp 文件，则将格式识别为 SHAPEFILE

## Capabilities

### New Capabilities
- `zip-shapefile-detection`: 增强 ZIP 文件格式检测，自动识别包含 Shapefile 的 ZIP 包

### Modified Capabilities
<!-- 空：无现有需求变更 -->

## Impact

**受影响文件**:
- `backend/src/main/java/com/gisplatform/service/FormatDetector.java`

## Non-goals

- 不修改 VectorFileFormat 枚举结构
- 不修改 VectorDataStoreFactory 的处理逻辑
- 不支持检测 ZIP 包内是否包含多个 Shapefile（仅检查是否存在至少一个 .shp 文件）
