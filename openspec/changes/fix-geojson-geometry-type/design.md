# Design: fix-geojson-geometry-type

## Technical Design

### Current Problem

```java
// 当前错误代码
String sql = "SELECT ST_AsGeoJSON(geom) as geojson, * FROM " + tableName;
// ...
Map<String, Object> geometry = new HashMap<>();
geometry.put("type", "Geometry");  // ❌ 错误：硬编码无效类型
geometry.put("coordinates", parseGeoJSONCoordinates(geojsonObj.toString()));
```

### Solution

直接使用 `ST_AsGeoJSON` 返回的完整 GeoJSON 对象，而不是手动构建：

```java
// 修复后代码
String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;
// ...
// 直接解析 ST_AsGeoJSON 返回的 JSON，获取 type 和 coordinates
JSONObject gj = JSONUtil.parseObj(gjObj.toString());

Map<String, Object> geometry = new HashMap<>();
geometry.put("type", gj.get("type"));      // ✅ Point/LineString/Polygon
geometry.put("coordinates", gj.get("coordinates"));
```

### Key Changes

1. 修改 SQL 别名：`ST_AsGeoJSON(geom) as gj`（避免与表字段冲突）
2. 使用 `JSONUtil.parseObj()` 解析返回的 GeoJSON 字符串
3. 从解析结果中提取 `type` 和 `coordinates` 字段

### Data Flow

```
数据库
  │
  │ ST_AsGeoJSON(geom) 返回:
  │ { "type": "Point", "coordinates": [116.4, 39.9] }
  ▼
parseObj() 解析 JSON
  │
  │ extract: type = "Point", coordinates = [...]
  ▼
构建 Feature
  │
  │ "geometry": { "type": "Point", "coordinates": [...] }
  ▼
返回有效 GeoJSON
  │
  ▼
OpenLayers 解析并渲染 ✅
```
