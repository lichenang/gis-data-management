# Tasks: fix-geojson-geometry-type

## Task 1: 修复 getDatasetAsGeoJSON 方法

**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

**修改内容**:

将错误的实现：
```java
String sql = "SELECT ST_AsGeoJSON(geom) as geojson, * FROM " + tableName;
// ...
Map<String, Object> geometry = new HashMap<>();
geometry.put("type", "Geometry");
geometry.put("coordinates", parseGeoJSONCoordinates(geojsonObj.toString()));
```

修改为：
```java
String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;
// ...
Object gjObj = row.get("gj");
if (gjObj == null) {
    continue;
}

JSONObject gj = JSONUtil.parseObj(gjObj.toString());

Map<String, Object> geometry = new HashMap<>();
geometry.put("type", gj.get("type"));
geometry.put("coordinates", gj.get("coordinates"));
```

同时删除不再需要的 `parseGeoJSONCoordinates` 方法。

## Task 2: 验证修复

1. 启动后端服务
2. 访问 `GET http://localhost:8088/api/v1/datasets/{id}/geojson`
3. 检查返回的 JSON 中 `features[].geometry.type` 是否为具体类型（如 "Point"）
4. 刷新地图查看页面，勾选已发布数据集
5. 确认地图上显示要素
