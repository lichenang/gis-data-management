## ADDED Requirements

### 模块划分

| 模块 | 位置 | 职责 |
|---|---|---|
| ExportController | `.../controller/ExportController.java` | 接收导出请求，按数据集类型和格式分发 |
| DatasetService（扩展） | `.../service/DatasetService.java` | 声明导出方法接口 |
| DatasetServiceImpl（扩展） | `.../service/impl/DatasetServiceImpl.java` | 实现 GeoJSON/KML/Shapefile 导出逻辑 |
| ExportUtil | `.../util/ExportUtil.java` | 工具方法（ZIP 打包、文件名清理、XML 转义） |
| 数据集管理页面 | `frontend/src/views/datasets/index.vue` | 操作列导出下拉按钮 |

### 数据流设计

```
用户点击"导出" → 前端创建 <a> 标签 GET 请求
    ↓
ExportController.exportDataset(id, format)
    ↓
查询 Dataset 实体 → type/tableName/minioKey
    ↓
分支判断
├── type=vector, format=geojson → 调用 getDatasetAsGeoJSON(id) → 写入 response
├── type=vector, format=kml    → 调用 getDatasetAsKML(id)  → 写入 response
├── type=vector, format=shapefile → 调用 exportShapefileAsZip(id, os) → ZIP 写入 response
└── type=raster, format=geotiff   → MinIO getObject 流式写入 response
```

### 接口列表

#### GET /api/v1/datasets/{id}/export?format=geojson

- **WHEN** 用户请求导出格式为 `geojson` 的矢量数据集
- **THEN** 返回 `Content-Type: application/geo+json` 的 GeoJSON 文件下载

#### Scenario: 导出矢量数据集为 GeoJSON

- **WHEN** 导出格式 `geojson`，数据集类型为 `vector`
- **THEN** 系统调用 `ST_AsGeoJSON()` 查询几何字段，返回 `.geojson` 附件下载

#### Scenario: 导出矢量数据集为 KML

- **WHEN** 导出格式 `kml`，数据集类型为 `vector`
- **THEN** 系统调用 `ST_AsKML()` 查询几何字段，组装 KML XML 文档，返回 `.kml` 附件下载

#### Scenario: 导出矢量数据集为 Shapefile ZIP

- **WHEN** 导出格式 `shapefile`，数据集类型为 `vector`
- **THEN** 系统通过 GeoTools PostGIS DataStore 读取要素，写入临时 Shapefile，打包为 ZIP 返回下载

#### Scenario: 导出影像数据集为 GeoTIFF

- **WHEN** 导出格式 `geotiff`，数据集类型为 `raster`
- **THEN** 系统从 MinIO 直接流式读取原始文件并返回 `.tiff` 附件下载

#### Scenario: 请求不存在的格式

- **WHEN** 导出格式不是 `geojson`/`shapefile`/`kml`/`geotiff` 中的任一值
- **THEN** 系统返回 400 错误，错误信息包含"不支持的导出格式"

#### Scenario: 格式与数据集类型不匹配

- **WHEN** 导出格式为 `geotiff` 但数据集类型为 `vector`
- **THEN** 系统返回 400 错误，错误信息包含"格式不匹配"

#### Scenario: 数据集不存在

- **WHEN** 导出的数据集 ID 不存在或被标记为已删除
- **THEN** 系统返回 404 错误，错误信息包含"数据集不存在"

#### Scenario: Shapefile 导出包含属性字段

- **WHEN** 导出格式为 `shapefile`
- **THEN** ZIP 包中的 `.dbf` 文件包含数据集的所有属性字段
