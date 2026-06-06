# import-projection-transform 规格说明

## 模块划分

- **转换层**: CrsTransformUtil - 提供 Geometry 投影转换方法
- **导入层**: MultiFormatImportServiceImpl - 调用转换逻辑写入 PostGIS

## ADDED Requirements

### Requirement: Shapefile 导入投影转换

MultiFormatImportService SHALL 在导入 Shapefile 时自动将坐标转换为 EPSG:4326。

#### Scenario: 导入 Web Mercator (EPSG:3857) Shapefile
- **WHEN** 导入原生坐标系为 EPSG:3857 的 Shapefile
- **THEN** 存储在 PostGIS 的坐标为 EPSG:4326 经纬度

#### Scenario: 导入 WGS84 (EPSG:4326) Shapefile
- **WHEN** 导入原生坐标系为 EPSG:4326 的 Shapefile
- **THEN** 存储在 PostGIS 的坐标保持 EPSG:4326（无转换）

#### Scenario: 导入中国 GCJ-02 偏移坐标
- **WHEN** 导入原生坐标系为 GCJ-02 的 Shapefile
- **THEN** 系统尝试转换，转换失败时记录警告日志但仍写入原始坐标

### Requirement: CrsTransformUtil 扩展

CrsTransformUtil SHALL 提供 Geometry 级别的投影转换方法。

#### Scenario: Geometry 转换
- **WHEN** 调用 `transformGeometry(geometry, sourceCrs)`
- **THEN** 返回转换后的 Geometry，坐标系为 EPSG:4326

#### Scenario: 同坐标系Geometry转换
- **WHEN** 调用 `transformGeometry(geometry, epsg4326Crs)` 且 sourceCrs 已是 EPSG:4326
- **THEN** 返回原始 Geometry 不变

## 数据流设计

```
importUsingDataStore(file, fileName, format, tableName)
    │
    ▼
DataStore dataStore = vectorDataStoreFactory.createDataStore(format, file)
    │
    ▼
FeatureSource.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem()
    │
    ▼
CoordinateReferenceSystem nativeCrs
    │
    ▼
MathTransform transform = CRS.findMathTransform(nativeCrs, targetCrs)
    │
    ▼
遍历 features:
    │
    ├── Geometry geom = feature.getDefaultGeometry()
    ├── Geometry transformed = JTS.transform(geom, transform)
    └── insertFeature(conn, tableName, transformed, propertyNames)
    │
    ▼
写入 PostGIS (EPSG:4326)
```

## 接口列表

| 方法 | 入参 | 返回值 | 说明 |
|------|------|--------|------|
| `CrsTransformUtil.transformGeometry(Geometry, CoordinateReferenceSystem)` | JTS Geometry, 源CRS | `Geometry` | 将 Geometry 转换到 EPSG:4326 |
