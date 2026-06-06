# 多格式矢量数据导出

## 模块划分

- **MultiFormatExportService**: 多格式导出服务，根据请求格式创建相应的 DataStore
- **ExportController**: 导出控制器，处理导出请求

## 数据流设计

```
请求导出 → 从 PostGIS 读取 FeatureSet → 根据格式创建目标 DataStore 
→ 转换为目标格式 → 返回下载文件
```

## 接口列表

### 导出数据集接口

**端点**: `GET /api/v1/datasets/{id}/export`

**参数**:
- id: Long (路径参数) - 数据集 ID
- format: String (查询参数) - 导出格式

**支持的 format**:
- geojson - GeoJSON 格式
- shapefile - Shapefile 格式（返回 zip 包）
- kml - KML 格式
- csv - CSV 格式（带坐标列）

**响应**: 文件下载 (Content-Disposition: attachment)

---

## ADDED Requirements

### Requirement: 用户可以导出为多种矢量格式

系统 SHALL 支持将 PostGIS 中的矢量数据导出为以下格式：GeoJSON、Shapefile (ZIP)、KML、CSV。

#### Scenario: 导出为 GeoJSON
- **WHEN** 用户请求导出为 geojson 格式
- **THEN** 系统从 PostGIS 读取数据，转换为 GeoJSON 格式返回下载

#### Scenario: 导出为 Shapefile
- **WHEN** 用户请求导出为 shapefile 格式
- **THEN** 系统将数据打包为包含 .shp、.shx、.dbf、.prj 的 zip 文件返回下载

#### Scenario: 导出为 KML
- **WHEN** 用户请求导出为 kml 格式
- **THEN** 系统将数据转换为 KML 格式返回下载

#### Scenario: 导出为 CSV
- **WHEN** 用户请求导出为 csv 格式
- **THEN** 系统将数据导出为 CSV 格式，几何以 WKT 形式存储在 geometry 列

#### Scenario: 导出空数据集
- **WHEN** 用户导出要素数量为 0 的数据集
- **THEN** 系统返回空文件或提示数据集为空

### Requirement: 导出文件编码正确

系统 SHALL 确保导出文件的编码正确，特别是中文字符。

#### Scenario: 导出包含中文属性
- **WHEN** 导出的数据包含中文字符属性
- **THEN** 输出文件使用 UTF-8 编码，中文显示正常

### Requirement: 大数据集导出

系统 SHALL 支持导出大数据集（万级要素）。

#### Scenario: 导出大数据集为 GeoJSON
- **WHEN** 用户导出一个包含 50000 个要素的数据集为 GeoJSON
- **THEN** 系统能够正常导出，不出现内存溢出

## MODIFIED Requirements

### Requirement: 导出接口 URL

**FROM:**
- GET /api/v1/datasets/{id}/export?format={format}

**TO:**
- 保持不变，但新增支持 format=csv 参数

### Requirement: 支持的导出格式

**FROM:**
- geojson, shapefile, kml (影像: geotiff)

**TO:**
- geojson, shapefile, kml, csv (影像: geotiff)

## 接口详细说明

| 接口 | 方法 | 参数 | 返回 |
|------|------|------|------|
| 导出数据集 | GET | id, format | 文件流 |

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 不支持的导出格式 |
| 404 | 数据集不存在 |
| 500 | 导出失败 |
