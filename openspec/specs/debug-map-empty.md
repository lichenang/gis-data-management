# 地图查看页面数据集不可见 - 诊断报告

## 问题描述

地图查看页面加载后，已发布的数据集在左侧图层列表显示，但勾选加载后地图区域为空，没有显示任何要素。

## 诊断结论

### 🔴 根因定位：后端 GeoJSON 导出接口存在严重 BUG

**文件**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`  
**方法**: `getDatasetAsGeoJSON(Long id)`  
**问题行**: 第 160 行

```java
// ❌ 当前错误代码
Map<String, Object> geometry = new HashMap<>();
geometry.put("type", "Geometry");  // ← 错误！应该是具体的几何类型
geometry.put("coordinates", parseGeoJSONCoordinates(geojsonObj.toString()));
```

**正确写法应该是**：
```java
// ✅ 正确代码
Map<String, Object> geometry = new HashMap<>();
String geometryType = getGeometryType(geojsonObj.toString()); // 从 ST_AsGeoJSON 结果中提取类型
geometry.put("type", geometryType);  // Point, LineString, Polygon 等
```

### 问题影响

GeoJSON 规范要求 geometry.type 必须是具体类型：
- Point
- LineString  
- Polygon
- MultiPoint
- MultiLineString
- MultiPolygon

当前代码硬编码为 "Geometry"（无效类型），导致 OpenLayers 的 GeoJSON 解析器无法识别，要素不会被渲染。

---

## 详细分析

### 1. 后端数据接口 ✅ 正常

**接口**: `GET /api/v1/datasets/published`

```java
@Override
public List<Dataset> listPublishedDatasets() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0));
}
```

**分析**: SQL 查询正确，返回已发布的数据集列表。

### 2. GeoJSON 导出接口 ❌ 存在 BUG

**接口**: `GET /api/v1/datasets/{id}/geojson`

当前实现的逻辑：
```java
String sql = "SELECT ST_AsGeoJSON(geom) as geojson, * FROM " + tableName;
```

**问题**：
1. `geometry.put("type", "Geometry")` - 硬编码了错误的几何类型
2. `ST_AsGeoJSON(geom)` 返回的 JSON 中已经包含了 type 字段，但代码没有正确提取

**ST_AsGeoJSON 返回格式示例**（应该这样解析）：
```json
{
  "type": "Point",
  "coordinates": [116.4, 39.9]
}
```

但当前代码构建的是：
```json
{
  "type": "Geometry",  // ← 错误！应该是 Point/LineString/Polygon
  "coordinates": [...]
}
```

### 3. 前端地图渲染 ✅ 逻辑正确

**文件**: `frontend/src/views/map/MapContainer.vue`

```typescript
const source = new VectorSource({
  url: `/api/v1/datasets/${layerInfo.id}/geojson`,
  format: new GeoJSON()
})
```

- ✅ 使用 VectorSource 加载 GeoJSON URL
- ✅ 使用 GeoJSON 格式解析器
- ✅ 坐标系设置为 EPSG:4326（View 配置正确）

### 4. 坐标系 ✅ 一致

- 数据导入：`ST_GeomFromGeoJSON()` 默认使用 EPSG:4326
- 数据存储：PostGIS 默认存储为 EPSG:4326
- 前端显示：`projection: 'EPSG:4326'`

---

## 数据流示意

```
数据库 (PostGIS)
    │
    │ ST_AsGeoJSON(geom) 返回:
    │ {
    │   "type": "Point",
    │   "coordinates": [116.4, 39.9]
    │ }
    ▼
DatasetServiceImpl.getDatasetAsGeoJSON()
    │  ❌ BUG: 硬编码 type = "Geometry"
    ▼
返回无效 GeoJSON:
{
  "type": "FeatureCollection",
  "features": [{
    "type": "Feature",
    "geometry": {
      "type": "Geometry",     ← 错误
      "coordinates": [...]
    }
  }]
}
    ▼
前端 MapContainer.vue
    │
    │ new GeoJSON().readFeatures()
    │ ❌ 无法解析无效 geometry type
    ▼
OpenLayers 不渲染 → 地图为空
```

---

## 修复方案

### 修复 DatasetServiceImpl.getDatasetAsGeoJSON()

```java
@Override
public String getDatasetAsGeoJSON(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("数据集不存在");
    }

    String tableName = dataset.getTableName();
    if (tableName == null || tableName.isEmpty()) {
        throw new RuntimeException("数据集无关联表");
    }

    // 使用 ST_AsGeoJSON 返回完整 GeoJSON 对象（包含 type）
    String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

    List<Map<String, Object>> features = new ArrayList<>();
    for (Map<String, Object> row : rows) {
        Object gjObj = row.get("gj");
        if (gjObj == null) {
            continue;
        }

        // 直接解析 ST_AsGeoJSON 返回的 JSON 对象
        JSONObject gj = JSONUtil.parseObj(gjObj.toString());
        
        Map<String, Object> feature = new HashMap<>();
        feature.put("type", "Feature");
        
        // properties 包含除 geometry 外的所有字段
        Map<String, Object> properties = new HashMap<>(row);
        properties.remove("gj");
        feature.put("properties", properties);
        
        // geometry 直接使用 ST_AsGeoJSON 的结果
        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", gj.get("type"));      // Point/LineString/Polygon
        geometry.put("coordinates", gj.get("coordinates"));
        feature.put("geometry", geometry);

        features.add(feature);
    }

    Map<String, Object> featureCollection = new HashMap<>();
    featureCollection.put("type", "FeatureCollection");
    featureCollection.put("features", features);

    return toJsonString(featureCollection);
}
```

---

## 验证步骤

1. 修复后端代码
2. 重新启动后端服务
3. 访问 `GET /api/v1/datasets/{id}/geojson`
4. 验证返回的 GeoJSON 中 geometry.type 是具体类型（如 "Point"）而非 "Geometry"
5. 刷新地图页面，勾选已发布数据集，应能看到要素

---

## 修改的文件

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `backend/.../DatasetServiceImpl.java` | ~160 | 正确解析 ST_AsGeoJSON 返回的 geometry type |
