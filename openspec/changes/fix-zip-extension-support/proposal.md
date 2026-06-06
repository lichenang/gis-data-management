## Why

系统支持通过 ZIP 包上传 Shapefile 文件，但当用户上传 `.zip` 文件时，格式校验逻辑拒绝该文件，返回"不支持的格式"错误。需要在 FormatDetector 或相关服务的格式校验逻辑中将 "zip" 添加为 Shapefile 导入的合法扩展名。

## What Changes

- 在 `GisDataParserServiceImpl.java` 的 `SUPPORTED_FORMATS` 数组中添加 `"zip"`（已完成于上一个变更）
- 确认 FormatDetector.formatIsValidForShapefile() 等相关方法已识别 zip

## Capabilities

### Modified Capabilities
- `shapefile-import`: 修复后支持通过 .zip 文件导入 Shapefile

## Impact

- **受影响文件**:
  - `backend/src/main/java/com/gisplatform/service/impl/GisDataParserServiceImpl.java`

## Non-goals

- 不修改 Shapefile 解析逻辑
- 不修改数据库 schema
