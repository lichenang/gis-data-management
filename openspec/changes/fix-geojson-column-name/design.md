# Design: fix-geojson-column-name

## Technical Design

### Current Problem

```java
// 错误代码 (DatasetServiceImpl.java:142)
String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;
//                                    ↑
//                            应该是 "geometry"
```

### Solution

修改 SQL 查询：
```java
// 修改后
String sql = "SELECT ST_AsGeoJSON(geometry) as gj FROM \"" + dbSchema + "\".\"" + tableName + "\"";
```

### Changes

1. **字段名**: `geom` → `geometry`
2. **Schema**: 添加 schema 前缀 `"public".tableName` 或使用配置的 dbSchema

### Additional Fix (Schema Prefix)

之前发现的 schema 问题也需要一并修复，否则跨 schema 查询会失败。

完整修复：
```java
@Value("${gis.datasource.schema:public}")
private String dbSchema;

// ...

String sql = "SELECT ST_AsGeoJSON(geometry) as gj FROM \"" + dbSchema + "\".\"" + tableName + "\"";
```

### Verification

1. 请求 `GET /api/v1/datasets/2/geojson`
2. 验证返回 200 且包含正确的 GeoJSON
3. 验证 geometry.type 是具体类型（Point/Polygon 等）
