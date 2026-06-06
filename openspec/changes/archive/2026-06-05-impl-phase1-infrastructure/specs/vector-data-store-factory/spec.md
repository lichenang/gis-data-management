# VectorDataStoreFactory 基础设施规格说明

## 模块划分

- **格式枚举层** (`VectorFileFormat`): 格式定义与检测
- **格式检测层** (`FormatDetector`): 文件格式自动识别
- **数据存储层** (`VectorDataStoreFactory`): GeoTools DataStore 创建

## 数据流设计

```
用户上传文件
      │
      ▼
FormatDetector.detect(file, filename)
      │
      ├─ 扩展名检测 → VectorFileFormat.fromExtension()
      ├─ JSON 内容检测 → detectJsonType()
      └─ ZIP 内容检测 → detectZipContent()
      │
      ▼
VectorFileFormat 格式枚举
      │
      ▼
VectorDataStoreFactory.createDataStore(format, file, filename)
      │
      ├─ SHAPEFILE → ShapefileDataStore (支持 ZIP 解压)
      ├─ GEOJSON → GeoJSONDataStore
      ├─ KML → KMLDataStore
      └─ ...
```

## 接口列表

| 接口 | 说明 |
|------|------|
| `FormatDetector.detect(MultipartFile, String)` | 自动识别文件格式 |
| `VectorDataStoreFactory.createDataStore(VectorFileFormat, MultipartFile, String)` | 创建 GeoTools DataStore |

## MODIFIED Requirements

### Requirement: VectorFileFormat 枚举扩展

VectorFileFormat 枚举 SHALL 支持格式检测和 GeoTools 集成。

#### Scenario: 格式支持检测
- **WHEN** 调用 `VectorFileFormat.fromExtension("shp")`
- **THEN** 返回 `SHAPEFILE` 格式

#### Scenario: GeoTools 支持检测
- **WHEN** 调用 `VectorFileFormat.GEOJSON.isGeoToolsSupported()`
- **THEN** 返回 `true`

#### Scenario: 所有格式枚举完整
- **WHEN** 系统需要支持所有矢量格式
- **THEN** VectorFileFormat 包含: GEOJSON, SHAPEFILE, KML, GML, GPX, CSV, WKT, TOPOJSON

### Requirement: FormatDetector 格式检测

FormatDetector SHALL 能自动识别上传文件的格式，不依赖文件扩展名。

#### Scenario: GeoJSON 文件检测
- **WHEN** 上传 `data.geojson` 文件
- **THEN** `detect()` 返回 `VectorFileFormat.GEOJSON`

#### Scenario: TopoJSON 文件检测
- **WHEN** 上传包含 `"Topology"` 的 JSON 文件
- **THEN** `detect()` 返回 `VectorFileFormat.TOPOJSON`

#### Scenario: Shapefile ZIP 包检测
- **WHEN** 上传包含 `.shp` 文件的 `.zip` 包
- **THEN** `detect()` 返回 `VectorFileFormat.SHAPEFILE`

### Requirement: VectorDataStoreFactory DataStore 创建

VectorDataStoreFactory SHALL 使用 GeoTools DataStoreFinder 创建各格式 DataStore。

#### Scenario: Shapefile DataStore 创建
- **WHEN** 调用 `createDataStore(SHAPEFILE, file, "data.zip")`
- **THEN** 返回可用的 ShapefileDataStore 实例

#### Scenario: GeoJSON DataStore 创建
- **WHEN** 调用 `createDataStore(GEOJSON, file, "data.geojson")`
- **THEN** 返回可用的 GeoJSONDataStore 实例

#### Scenario: ZIP 包自动解压
- **WHEN** 上传的 Shapefile 是 ZIP 格式
- **THEN** Factory 自动解压并从 .shp 文件创建 DataStore
