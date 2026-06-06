# 多格式矢量数据导入

## 模块划分

- **FormatDetector**: 格式自动识别模块，根据文件扩展名和文件内容自动识别矢量格式
- **VectorDataStoreFactory**: GeoTools DataStore 工厂类，统一创建各格式的 DataStore
- **MultiFormatImportService**: 多格式导入服务，协调整个导入流程
- **CoordinateTransformUtil**: 坐标系转换工具类

## 数据流设计

```
上传文件 → FormatDetector 识别格式 → VectorDataStoreFactory 创建 DataStore 
→ 读取 FeatureSet → 坐标转换 → 写入 PostGIS → 返回导入结果
```

## 接口列表

### 1. 解析文件接口

**端点**: `POST /api/v1/datasets/parse`

**请求**:
- Content-Type: multipart/form-data
- file: File (必需)

**响应** (200):
```json
{
  "code": 200,
  "data": {
    "success": true,
    "format": "GeoJSON",
    "geometryType": "MultiPolygon",
    "featureCount": 5000,
    "srs": "EPSG:4326",
    "bounds": [minX, minY, maxX, maxY],
    "properties": ["name", "population", "area"],
    "tableName": "dataset_example_1234"
  }
}
```

### 2. 导入数据集接口

**端点**: `POST /api/v1/datasets/import`

**请求**:
- Content-Type: multipart/form-data
- file: File (必需)
- name: String (必需) - 数据集名称
- description: String (可选)
- srs: String (可选, 默认 EPSG:4326)

**响应** (200):
```json
{
  "code": 200,
  "data": {
    "success": true,
    "datasetId": 123,
    "importedCount": 5000,
    "geometryType": "MultiPolygon",
    "sourceFormat": "GeoJSON",
    "bounds": [minX, minY, maxX, maxY]
  }
}
```

---

## ADDED Requirements

### Requirement: 用户可以导入多种矢量格式数据

系统 SHALL 支持导入以下矢量格式：GeoJSON、Shapefile、KML/KMZ、GML、GPX、CSV、WKT、TopoJSON。

#### Scenario: 成功导入 GeoJSON 文件
- **WHEN** 用户上传 .geojson 文件并提交导入
- **THEN** 系统解析文件内容，提取几何和属性信息，存储到 PostGIS 数据库，返回成功结果和导入数量

#### Scenario: 成功导入 Shapefile zip 包
- **WHEN** 用户上传包含 .shp/.shx/.dbf/.prj 的 .zip 文件并提交导入
- **THEN** 系统解压文件，使用 GeoTools ShapefileDataStore 解析，存储到数据库，返回成功结果

#### Scenario: 成功导入 KML 文件
- **WHEN** 用户上传 .kml 文件并提交导入
- **THEN** 系统使用 GeoTools KMLDataStore 解析，转换为几何后存储

#### Scenario: 自动识别文件格式
- **WHEN** 用户上传文件（扩展名正确但内容可能有误）
- **THEN** 系统识别文件格式，如果无法识别返回明确错误信息

#### Scenario: 坐标系转换
- **WHEN** 用户指定目标坐标系（如 EPSG:3857）导入非该坐标系的数据
- **THEN** 系统自动进行坐标转换后再存储到数据库

#### Scenario: 动态属性字段
- **WHEN** 导入的文件包含不同数量和类型的属性字段
- **THEN** 系统根据实际数据动态创建表字段，最大兼容所有属性

### Requirement: 用户可以预览导入文件信息

在正式导入前，用户 SHALL 能够预览文件的格式、几何类型、要素数量、边界框等信息。

#### Scenario: 预览 GeoJSON 文件信息
- **WHEN** 用户选择 GeoJSON 文件后
- **THEN** 系统显示格式类型、几何类型、要素数量、坐标系、边界框、属性字段列表

#### Scenario: 预览 Shapefile 信息
- **WHEN** 用户选择 Shapefile 文件（可能需要多文件）
- **THEN** 系统显示检测到的 Shapefile 组件文件、几何类型、要素数量

### Requirement: 不支持的格式返回明确错误

系统 SHALL 对不支持的文件格式返回清晰的错误信息。

#### Scenario: 上传不支持的格式
- **WHEN** 用户上传 .dxf 等不支持的格式
- **THEN** 系统返回 400 错误，提示不支持该格式，列出支持的格式列表

#### Scenario: Shapefile 文件不完整
- **WHEN** 用户上传 Shapefile 但缺少必要文件（.shp、.shx、.dbf 任一缺失）
- **THEN** 系统返回 400 错误，提示缺少哪些必要文件

## MODIFIED Requirements

### Requirement: 上传文件大小限制

**FROM:**
- 文件大小限制：无明确限制

**TO:**
- 单个文件大小不超过 500MB
- 超过限制返回 413 错误

## 接口详细说明

| 接口 | 方法 | 参数 | 返回 |
|------|------|------|------|
| 解析文件 | POST | file: File | ParseResult |
| 导入数据 | POST | file, name, description?, srs? | ImportResult |

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 不支持的格式、文件不完整、参数错误 |
| 413 | 文件大小超限 |
| 500 | 服务器内部错误 |
