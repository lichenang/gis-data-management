## Context

当前 Shapefile 上传支持两种形式：
1. 直接上传 `.shp` 文件
2. 上传包含 `.shp/.shx/.dbf/.prj` 的 `.zip` 包

然而，当用户上传 `.zip` 文件时，系统返回"不支持的格式"错误。

**问题定位**：`GisDataParserServiceImpl.parseFile()` 方法中使用 `getFileExtension()` 获取扩展名，然后检查是否在 `SUPPORTED_FORMATS` 数组中。当前 `SUPPORTED_FORMATS = {"shp", "geojson", "json"}`，不包含 `zip`。

由于 `FormatDetector` 的检测（`VectorFileFormat.fromExtension("zip")` 返回 `SHAPEFILE`）发生在 `isSupportedFormat()` 检查之后，用户在格式检测通过前就被拒绝了。

## Goals / Non-Goals

**Goals:**
- 将 `zip` 添加到 `GisDataParserServiceImpl.SUPPORTED_FORMATS` 数组中

**Non-Goals:**
- 不修改 FormatDetector 逻辑
- 不添加新的格式支持
- 不修改 Shapefile 解析实现

## Risks / Trade-offs

无显著风险。这是一个单行代码修改。

## Migration Plan

1. 修改 `GisDataParserServiceImpl.java` 的 `SUPPORTED_FORMATS` 数组
2. 编译验证：`mvn compile`
3. 测试上传 ZIP 包
