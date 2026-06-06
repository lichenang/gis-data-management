# 诊断报告：Shapefile 导入坐标投影转换问题

## 问题现象

用户导入 Shapefile 后，通过 GeoJSON 接口 `/api/v1/datasets/{id}/geojson` 返回的坐标是米制投影坐标（如 `38453552.737`），而非 EPSG:4326 经纬度坐标（如 `116.4, 39.9`）。

## 问题根因

### 导入流程分析

```
MultiFormatImportServiceImpl.importUsingDataStore()
    │
    ├── VectorDataStoreFactory.createShapefileDataStore()
    │       │
    │       └── DataStoreFinder.getDataStore(params)
    │               │
    │               └── GeoTools ShapefileDataStore
    │                       │                    │
    │                       ├── 读取 .shp 文件   │
    │                       └── 读取 .prj 文件   │ → 获取原生 CRS
    │
    ├── featureSource.getFeatures()  →  返回原始坐标（原生 CRS）
    │
    ├── geometry = feature.getDefaultGeometry()  →  JTS Geometry（未转换）
    │
    └── writeWkb(geometry) → ST_GeomFromWKB(bytes)  →  PostGIS
                                │
                                ▼
                     存储的坐标仍是原生 CRS（如 EPSG:3857）
```

### 问题代码位置

**文件**: `MultiFormatImportServiceImpl.java`

```java
// 第 185-190 行：读取要素，未做投影转换
try (org.geotools.feature.FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures().features()) {
    while (iterator.hasNext()) {
        SimpleFeature feature = iterator.next();
        insertFeature(conn, tableName, feature, propertyNames);  // ← 问题：无 CRS 转换
        count++;
    }
}
```

```java
// 第 231-234 行：直接写入 WKB，未做投影转换
Object geom = feature.getDefaultGeometry();
if (geom != null && geom instanceof org.locationtech.jts.geom.Geometry) {
    org.locationtech.jts.geom.Geometry geometry = (org.locationtech.jts.geom.Geometry) geom;
    stmt.setBytes(1, writeWkb(geometry));  // ← 问题：写入原始坐标
}
```

## 缺失的步骤

| 步骤 | 状态 | 说明 |
|------|------|------|
| 1. 获取 Shapefile 原生 CRS | ✓ 已有 | `DataStore.getFeatureSource().getSchema().getCoordinateReferenceSystem()` |
| 2. 定义目标 CRS (EPSG:4326) | ✗ 缺失 | 需要定义常量 |
| 3. 创建坐标转换 MathTransform | ✗ 缺失 | 需要使用 `CRS.findMathTransform()` |
| 4. 对每个 Geometry 进行转换 | ✗ 缺失 | 需要调用 `MathTransform.transform()` |
| 5. 写入转换后的 Geometry | ✗ 缺失 | 当前直接写入原始 geometry |

## 已有资源

### CrsTransformUtil

项目已有 `CrsTransformUtil.java`，但仅用于 `transformExtentToWgs84()`，不支持单个 Geometry 转换：

```java
// CrsTransformUtil.java - 第 14 行
private static final String TARGET_CRS = "EPSG:4326";
```

## 修复方案

### 方案 A：在导入时转换（推荐）

在 `MultiFormatImportServiceImpl.importUsingDataStore()` 中：

1. 获取 FeatureSource 的原生 CRS
2. 创建 `MathTransform` 从原生 CRS → EPSG:4326
3. 对每个读取的 Geometry 应用转换
4. 写入转换后的 Geometry

```java
// 获取原生 CRS
CoordinateReferenceSystem nativeCRS = featureSource.getSchema().getGeometryDescriptor()
    .getCoordinateReferenceSystem();

// 创建转换
MathTransform transform = CRS.findMathTransform(nativeCRS, CRS.decode("EPSG:4326"));

// 转换每个 geometry
for (SimpleFeature feature : features) {
    Geometry geom = (Geometry) feature.getDefaultGeometry();
    Geometry transformed = JTS.transform(geom, transform);
    // 写入 transformed
}
```

### 方案 B：导出时转换

在 `DatasetServiceImpl.getDatasetAsGeoJSON()` 中：

```java
// 使用 PostGIS 函数转换
String sql = "SELECT ST_AsGeoJSON(ST_Transform(geometry, 4326)) as gj FROM...";
```

**缺点**：每次导出都做转换，性能损耗；数据在库中仍是非目标 CRS

### 方案 C：存储时用 PostGIS 函数转换

在 SQL 写入时使用 `ST_Transform`：

```sql
INSERT INTO table (geometry) VALUES (ST_Transform(ST_GeomFromWKB(?), 4326))
```

**缺点**：需要先知道原生 CRS，且在每次导入时做转换

## 推荐方案

**方案 A（导入时转换）** 是最佳选择，因为：
- 数据在数据库中已经是目标 CRS
- 导出查询简单高效
- 所有后续处理（空间查询、地图显示）都直接使用 EPSG:4326

## 修复任务清单

1. 在 `MultiFormatImportServiceImpl` 中添加 CRS 转换逻辑
2. 注入或使用 `CrsTransformUtil` 的转换能力
3. 转换 Geometry 后再写入 PostGIS
4. 在 Dataset 表中记录数据的原始 CRS（如有需要回滚）

## 影响范围

| 文件 | 修改内容 |
|------|----------|
| `MultiFormatImportServiceImpl.java` | 添加 CRS 检测和转换逻辑 |
| 可能需要 `CrsTransformUtil.java` 扩展 | 添加 `transformGeometry()` 方法 |
