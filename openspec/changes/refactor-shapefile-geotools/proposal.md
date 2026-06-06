## Why

当前 Shapefile 导入使用手写 ByteBuffer 二进制解析，多次调整记录头偏移量仍无法正确读取几何数据，导致 PostGIS 表为空。改用项目已有依赖 GeoTools 32.x 的 ShapefileDataStore 进行标准解析，消除脆弱的手写代码。

## What Changes

1. **使用 GeoTools ShapefileDataStore 替换手写解析**
   - 删除 parseShpFile()、parseShpHeader()、parseShpRecord() 等手写二进制解析方法
   - 删除 ShapefileHeader、ShapefileData 内部类
   - 使用 ShapefileDataStoreFactory 创建 DataStore，从 .shp 文件读取特征

2. **修复 ZIP 解压提取到临时文件**
   - extractShapefileFromZip() 改为提取到临时目录
   - 使用 temp .shp 文件路径创建 ShapefileDataStore

3. **清理未使用/错误的 import**
   - 删除不再使用的 java.nio.ByteBuffer、ByteOrder
   - 保留 GeoTools 相关的正确 import

## Capabilities

### New Capabilities
- `shapefile-geotools-parsing`: 使用 GeoTools 标准 API 解析 Shapefile

### Modified Capabilities
- `multi-format-vector-import`: 修复并改进 Shapefile 导入功能

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportService.java`
- 继续使用已存在的 geotools 依赖
- 无前端影响

## Non-Goals

- 不修改 API 接口契约
- 不改变 GeoJSON 导入逻辑
