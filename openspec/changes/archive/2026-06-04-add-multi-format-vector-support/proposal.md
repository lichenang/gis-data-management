## Why

当前系统仅支持 GeoJSON 格式的矢量数据导入，无法满足用户多样化的数据导入需求。用户经常需要导入 Shapefile、KML/KMZ、GPX 等常见 GIS 格式的数据。此外，导出功能也仅限于 GeoJSON，无法满足不同场景下的数据输出需求。实现多格式矢量数据支持将大幅提升系统的实用性和竞争力。

## What Changes

1. **新增后端多格式导入服务**
   - 使用 GeoTools DataStore 统一解析多种矢量格式
   - 支持格式：Shapefile、KML/KMZ、GeoJSON、TopoJSON、GML、GPX、CSV、WKT
   - 自动识别文件格式
   - 支持 Shapefile zip 包上传和多文件上传
   - 自动坐标系转换

2. **扩展前端上传组件**
   - 支持多种格式文件选择
   - 显示识别到的格式和元数据信息
   - 支持 Shapefile 多文件上传

3. **新增导出格式支持**
   - 扩展导出为支持 Shapefile (ZIP)、KML、CSV 格式

4. **数据存储层改进**
   - 统一的 PostGIS geometry 字段存储
   - 动态属性字段处理

## Capabilities

### New Capabilities
- `multi-format-vector-import`: 使用 GeoTools 统一解析多种矢量格式数据并导入 PostGIS
- `multi-format-vector-export`: 多格式矢量数据导出功能

### Modified Capabilities
- 无（data-export 相关规格仅实现层面变更，需求层面未变化）

## Impact

### 受影响的后端模块
- `com.gisplatform.service.impl.GisDataParserServiceImpl` - 需重构为多格式支持
- 新增 `com.gisplatform.service.VectorDataStoreFactory`
- 新增 `com.gisplatform.service.FormatDetector`
- 新增 `com.gisplatform.service.MultiFormatExportService`

### 受影响的 API
- `POST /api/v1/datasets/parse` - 支持更多格式
- `POST /api/v1/datasets/import` - 支持更多格式
- `GET /api/v1/datasets/{id}/export` - 新增 CSV 导出格式

### 受影响的前端模块
- `frontend/src/views/datasets/index.vue` - 上传组件扩展
- `frontend/src/api/dataset.ts` - 可能需要扩展

### 依赖变更
- 可能需要添加 `gt-geojson`、`gt-csv` 等 GeoTools 模块

### 非目标
- 不实现栅格数据的导入格式扩展
- 不实现数据库已有数据的格式批量转换
- 不实现服务端矢量切片动态渲染为其他格式
