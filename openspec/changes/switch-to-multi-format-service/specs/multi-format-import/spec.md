# multi-format-import 规格说明

## 模块划分

- **检测层**: FormatDetector - 深度检测 ZIP 包内文件类型
- **存储层**: VectorDataStoreFactory - 根据格式创建对应的 DataStore
- **导入层**: MultiFormatImportService - 统一的解析和导入接口

## ADDED Requirements

### Requirement: 多格式文件解析

MultiFormatImportService SHALL 支持解析多种矢量数据格式，包括 Shapefile（单文件 和 ZIP 包）、GeoJSON、KML、KMZ、GML、GPX、CSV。

#### Scenario: 解析 Shapefile ZIP 包
- **WHEN** 调用 `parseFile(file, "data.zip")` 且 ZIP 包内包含 .shp 文件
- **THEN** 返回 `GisDataParseResult`，success=true，format=SHAPEFILE

#### Scenario: 解析普通 Shapefile
- **WHEN** 调用 `parseFile(file, "data.shp")`
- **THEN** 返回 `GisDataParseResult`，success=true，format=SHAPEFILE

#### Scenario: 解析 GeoJSON 文件
- **WHEN** 调用 `parseFile(file, "data.geojson")`
- **THEN** 返回 `GisDataParseResult`，success=true，format=GEOJSON，包含 geometryType、bounds、featureCount

#### Scenario: 解析不支持的格式
- **WHEN** 调用 `parseFile(file, "data.xyz")`
- **THEN** 返回 `GisDataParseResult`，success=false，message 包含"不支持的格式"

### Requirement: 多格式数据导入

MultiFormatImportService SHALL 支持将多种矢量数据格式导入到 PostGIS 数据库。

#### Scenario: 导入 Shapefile ZIP 包
- **WHEN** 调用 `importToPostGIS(file, "data.zip", "mydataset", "EPSG:4326")` 且 ZIP 包内包含 Shapefile
- **THEN** 返回 `DatasetImportResult`，success=true，datasetId 不为 null，importedCount > 0

#### Scenario: 导入 GeoJSON 文件
- **WHEN** 调用 `importToPostGIS(file, "data.geojson", "mydataset", "EPSG:4326")`
- **THEN** 返回 `DatasetImportResult`，success=true，datasetId 不为 null，importedCount > 0

#### Scenario: 导入时数据集名称为空
- **WHEN** 调用 `importToPostGIS(file, "data.shp", "", "EPSG:4326")`
- **THEN** 返回 `DatasetImportResult`，success=false，message 包含"数据集名称"

## 数据流设计

```
                    ┌─────────────────────────────────┐
                    │   MultiFormatImportService      │
                    │  parseFile() / importToPostGIS() │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    ▼                              ▼
           ┌───────────────┐            ┌─────────────────┐
           │FormatDetector │            │VectorDataStore  │
           │  .detect()    │            │Factory          │
           │               │            │.createDataStore()│
           └───────┬───────┘            └────────┬────────┘
                   │                             │
                   ▼                             ▼
           VectorFileFormat              DataStore
           (SHAPEFILE/KML/etc)          (具体格式的存储)
```

## 接口列表

| 方法 | 入参 | 返回值 | 说明 |
|------|------|--------|------|
| `parseFile(MultipartFile file, String fileName)` | 文件对象, 文件名 | `GisDataParseResult` | 解析文件并返回元数据 |
| `importToPostGIS(MultipartFile file, String fileName, String datasetName, String targetSrs)` | 文件, 文件名, 数据集名, 坐标系 | `DatasetImportResult` | 导入数据到 PostGIS |
| `getSupportedFormats()` | 无 | `String[]` | 返回支持的格式列表 |

## 支持的格式

| 格式 | 扩展名 | 导入支持 | 解析支持 |
|------|--------|----------|----------|
| Shapefile | .shp, .zip | ✓ | ✓ |
| GeoJSON | .geojson, .json | ✓ | ✓ |
| KML | .kml | ✓ | ✓ |
| KMZ | .kmz | ✓ | ✓ |
| GML | .gml | ✓ | ✓ |
| GPX | .gpx | ✓ | ✓ |
| CSV | .csv | ✓ | ✓ |
