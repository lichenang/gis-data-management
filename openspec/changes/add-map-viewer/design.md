# Design: add-map-viewer

## Technical Design

### Backend Implementation

#### 1. DatasetService 新增方法

**接口**: `DatasetService.java`
```java
List<Dataset> listPublishedDatasets();
String getDatasetAsGeoJSON(Long id);
```

**DatasetServiceImpl.java** 实现：
- `listPublishedDatasets()`: 使用 MyBatis-Plus 查询 `status='published'` 的数据集
- `getDatasetAsGeoJSON(id)`: 
  - 根据 id 查询数据集获取 tableName
  - 使用 JDBC 直接查询 PostGIS: `SELECT ST_AsGeoJSON(geom) as geojson, * FROM {tableName}`
  - 组装为 FeatureCollection 格式返回

#### 2. DatasetController 新增接口

**接口**: `/api/v1/datasets/published`
```java
@GetMapping("/published")
@Operation(summary = "获取已发布数据集列表")
public R<List<Dataset>> listPublished();
```

**接口**: `/api/v1/datasets/{id}/geojson`
```java
@GetMapping("/{id}/geojson")
@Operation(summary = "获取数据集 GeoJSON")
public R<String> getGeoJSON(@PathVariable Long id);
```

### Frontend Implementation

#### 1. 路由配置

**File**: `frontend/src/router/index.ts`
```typescript
{
  path: '/map',
  name: 'MapViewer',
  component: () => import('@/views/map/index.vue'),
  meta: { title: '地图查看', requiresAuth: true }
}
```

#### 2. 组件结构

```
frontend/src/views/map/
├── index.vue          # 主页面（左右布局）
├── LayerPanel.vue     # 左侧图层列表
└── MapContainer.vue   # OpenLayers 地图容器
```

#### 3. MapContainer 核心逻辑

- 初始化 OpenLayers Map，使用 OSM 作为底图
- 提供 `addLayer(dataset)` / `removeLayer(datasetId)` 方法
- 使用 `VectorSource` + `VectorLayer` 加载 GeoJSON
- 使用 `Select` interaction 处理要素点击事件
- 配置 Popup 显示要素属性

#### 4. LayerPanel 逻辑

- 调用 `/api/v1/datasets/published` 获取已发布数据集
- 使用 Checkbox 勾选控制图层的加载/卸载
- 与 MapContainer 通过 props/emit 通信

## Data Flow

```
User checks layer checkbox
        │
        ▼
LayerPanel emits "layer-change" event
        │
        ▼
MapContainer receives event, calls addLayer()
        │
        ▼
MapContainer fetches GeoJSON from API
        │
        ▼
GET /api/v1/datasets/{id}/geojson
        │
        ▼
Backend queries PostGIS, returns GeoJSON
        │
        ▼
OpenLayers renders GeoJSON on map
```

## Edge Cases

1. 数据集无几何数据：返回空 FeatureCollection
2. 数据集 tableName 不存在：返回 404 错误
3. GeoJSON 数据过大：考虑后端分页或限制返回数量
4. 坐标系非 EPSG:4326：后端使用 ST_Transform 转换
