## Why

当前的多格式矢量导入功能中的 Shapefile 导入使用手写二进制解析（ByteBuffer 直接解析 .shp 文件），经过多次调试偏移量仍无法正确读取几何数据。改用 GeoTools 32.x 提供的标准 ShapefileDataStore API 进行解析，一次性解决问题并提高代码可维护性。

## What Changes

1. **删除手写二进制解析代码**
   - 删除 MultiFormatImportService 中的 parseShpFile() 方法
   - 删除相关的 parseShpRecord(), parsePoint(), parsePolygon() 等辅助方法
   - 删除 ShapefileHeader, ShapefileData 等内部类

2. **使用 GeoTools ShapefileDataStore 解析**
   - 使用 ShapefileDataStoreFactory 创建 DataStore
   - 从 .shp 文件或 .zip 中提取 .shp 后创建 DataStore
   - 使用 SimpleFeatureSource 获取特征集合
   - 遍历 SimpleFeature 读取几何和属性

3. **更新导入逻辑**
   - importShapefile() 改用 GeoTools 读取数据
   - parseShapefile() 改用 GeoTools 获取元数据
   - 保留 insertShapefileRecords() 使用 WKB 写入 PostGIS

## Capabilities

### New Capabilities
- `shapefile-geotools-parsing`: 使用 GeoTools ShapefileDataStore 进行标准化 Shapefile 解析

### Modified Capabilities
- `multi-format-vector-import`: 修复 Shapefile 导入功能

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportService.java` - 重构解析逻辑
- 依赖: geotools 32.x (已存在)
- 无前端影响

## Non-Goals

- 不修改 API 接口契约
- 不改变现有的 GeoJSON 导入逻辑
- 暂不修改 KML 解析（保持现状）
