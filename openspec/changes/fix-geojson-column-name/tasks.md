# Tasks: fix-geojson-column-name

## Task 1: 修复字段名和 schema

**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

**修改内容**:

1. 在类中添加 dbSchema 配置：
```java
@Value("${gis.datasource.schema:public}")
private String dbSchema;
```

2. 修改 getDatasetAsGeoJSON 方法中的 SQL：
```java
// 修改前
String sql = "SELECT ST_AsGeoJSON(geom) as gj FROM " + tableName;

// 修改后
String sql = "SELECT ST_AsGeoJSON(geometry) as gj FROM \"" + dbSchema + "\".\"" + tableName + "\"";
```

## Task 2: 验证修复

1. 启动后端服务
2. 访问 `GET /api/v1/datasets/2/geojson`
3. 验证返回 200 状态码
4. 检查 geometry.type 是否为具体类型（Point/Polygon 等）
5. 刷新地图页面验证显示正确
