## Context

`MultiFormatImportServiceImpl.importUsingDataStore()` 导入 Shapefile 时存在问题：

```
当前流程：
┌──────────────────────────────────────────────────────────────┐
│  GeoTools DataStore.getFeatureSource()                       │
│       │                                                     │
│       ▼                                                     │
│  featureSource.getSchema().getGeometryDescriptor()          │
│       │                                                     │
│       ▼                                                     │
│  获取原生 CRS (如 EPSG:3857)                                │
│       │                                                     │
│       ▼                                                     │
│  feature.getDefaultGeometry() → JTS Geometry (原生 CRS)     │
│       │                                                     │
│       ▼                                                     │
│  writeWkb(geometry) → ST_GeomFromWKB(bytes) → PostGIS       │
│       │                                                     │
│       ▼                                                     │
│  PostGIS 存储：原生 CRS（如 3857 米制坐标）                 │
└──────────────────────────────────────────────────────────────┘

期望流程：
┌──────────────────────────────────────────────────────────────┐
│  GeoTools DataStore.getFeatureSource()                       │
│       │                                                     │
│       ▼                                                     │
│  获取原生 CRS (如 EPSG:3857)                                │
│       │                                                     │
│       ▼                                                     │
│  创建 MathTransform (原生 → EPSG:4326)                      │
│       │                                                     │
│       ▼                                                     │
│  JTS.transform(geometry, mathTransform) → EPSG:4326 几何   │
│       │                                                     │
│       ▼                                                     │
│  writeWkb(transformed) → ST_GeomFromWKB(bytes) → PostGIS    │
│       │                                                     │
│       ▼                                                     │
│  PostGIS 存储：EPSG:4326 (经纬度坐标)                       │
└──────────────────────────────────────────────────────────────┘
```

## Goals / Non-Goals

**Goals:**
- 在导入时自动将 Shapefile 坐标转换为 EPSG:4326
- 所有存储在 PostGIS 的矢量数据统一为 EPSG:4326 坐标系
- GeoJSON 导出自 PostGIS 时直接返回 EPSG:4326 坐标

**Non-Goals:**
- 不修改 GeoJSON 导出查询逻辑
- 不处理 KML/GML/GPX 等其他格式的投影转换
- 不实现 CRS 检测失败时的回退逻辑

## Decisions

### Decision 1: 在导入时转换而非导出时转换

**方案**：在 `MultiFormatImportServiceImpl.importUsingDataStore()` 中使用 GeoTools MathTransform 转换坐标后再写入

**理由**：
- 数据在 PostGIS 中已是目标坐标系，查询性能最优
- 后续所有空间操作（ST_Intersects、ST_DWithin 等）直接使用 EPSG:4326
- 不需要在每次导出时做转换

### Decision 2: 复用 CrsTransformUtil 扩展

**方案**：扩展 `CrsTransformUtil` 添加 Geometry 转换方法，或在 `MultiFormatImportServiceImpl` 内部实现

**理由**：
- `CrsTransformUtil` 已定义 `TARGET_CRS = "EPSG:4326"`
- `CRS.findMathTransform()` 已有完整实现
- 保持一致性，减少重复代码

### Decision 3: 获取原生 CRS 的方式

**方案**：通过 `featureSource.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem()`

**理由**：
- GeoTools DataStore 的 Schema 包含完整的 CRS 信息
- 从 .prj 文件读取的原生 CRS 可通过此 API 获取

## 实现方案

### 步骤 1: 在 CrsTransformUtil 添加 Geometry 转换方法

```java
public static Geometry transformGeometry(Geometry geometry, CoordinateReferenceSystem sourceCrs) {
    if (geometry == null) {
        return null;
    }
    CoordinateReferenceSystem targetCrs = CRS.decode(TARGET_CRS);
    if (sourceCrs.equals(targetCrs)) {
        return geometry;
    }
    MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs);
    return JTS.transform(geometry, transform);
}
```

### 步骤 2: 在 MultiFormatImportServiceImpl 中使用

```java
// 获取原生 CRS
CoordinateReferenceSystem nativeCrs =
    featureSource.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();

// 遍历并转换
while (iterator.hasNext()) {
    SimpleFeature feature = iterator.next();
    Geometry geom = (Geometry) feature.getDefaultGeometry();
    Geometry transformed = CrsTransformUtil.transformGeometry(geom, nativeCrs);
    insertFeature(conn, tableName, transformed, propertyNames);
}
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 原生 CRS 获取失败 | 使用 try-catch，失败时跳过转换并记录日志 |
| 大数据量转换性能 | 批量提交减少事务开销 |
| 坐标转换精度损失 | GeoTools MathTransform 保证亚毫米精度 |
