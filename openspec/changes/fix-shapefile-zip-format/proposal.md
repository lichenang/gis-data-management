## Why

当前系统支持 Shapefile (`.shp`) 格式上传，但当用户上传包含 Shapefile 文件的 ZIP 包 (`.zip`) 时，系统返回"不支持的格式"错误。原因是 `FormatDetector.isZipShapefile()` 仅基于文件扩展名判断，未将 `.zip` 识别为 Shapefile 的有效载体。

## What Changes

- 在 `FormatDetector.java` 中将 `.zip` 添加到 Shapefile 相关格式支持列表

## Capabilities

### Modified Capabilities
- `shapefile-import`: 修复后支持通过 ZIP 包上传 Shapefile（包含 .shp/.shx/.dbf/.prj 等文件）

## Impact

- **受影响文件**:
  - `backend/src/main/java/com/gisplatform/service/FormatDetector.java`

## Non-goals

- 不修改 Shapefile 解析逻辑
- 不添加新的导出功能
- 不修改数据库 schema
