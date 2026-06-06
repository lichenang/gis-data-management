## Why

当前系统导入 Shapefile ZIP 文件时硬编码使用 UTF-8 编码，导致中文文件名或中文属性的 ZIP 包解压失败。这是由于不同来源的 Shapefile（国内 GIS 软件导出、国外软件导出）可能使用不同的字符编码（GBK、GB18030、ISO-8859-1 等），而当前实现未处理编码识别问题。

## What Changes

1. **修复 FormatDetector ZIP 编码检测**
   - 移除硬编码的 UTF-8，改为自动尝试多种编码（UTF-8、GBK、GB18030、ISO-8859-1、系统默认编码）
   - 使用 fallback 机制确保不同编码的 ZIP 都能正确识别内容

2. **修复 MultiFormatImportService ZIP 解压**
   - 将解压方法重构为基于 byte[] 缓存的方式，避免流重复打开导致的编码问题
   - 添加编码自动检测逻辑，确保从 ZIP 中提取的 .shp 文件名和内容正确

3. **统一编码处理工具类**
   - 提取公共的编码检测逻辑到 FormatDetector 中
   - 确保解析和解压使用一致的编码策略

## Capabilities

### New Capabilities
- `multi-format-vector-import`: 完善多格式矢量数据导入功能，支持不同编码的 Shapefile ZIP 文件

### Modified Capabilities
（无，现有 spec 不需要修改）

## Impact

- `backend/src/main/java/com/gisplatform/service/FormatDetector.java` - 添加编码自动检测逻辑
- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportService.java` - 修复 ZIP 解压编码处理
- 无前端影响

## Non-Goals

- 不修改 GeoJSON、KML 等其他格式的解析逻辑
- 不添加新的矢量导入格式
- 不改变现有的 API 接口契约
