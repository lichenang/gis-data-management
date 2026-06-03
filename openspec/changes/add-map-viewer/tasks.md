# Tasks: add-map-viewer

## Backend Tasks

### Task 1: DatasetService 新增 listPublishedDatasets 方法

**File**: `backend/src/main/java/com/gisplatform/service/DatasetService.java`

在接口中添加方法签名：
```java
List<Dataset> listPublishedDatasets();
```

**File**: `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`

实现方法：
```java
@Override
public List<Dataset> listPublishedDatasets() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0));
}
```

### Task 2: DatasetService 新增 getDatasetAsGeoJSON 方法

**File**: `DatasetService.java`

添加方法签名：
```java
String getDatasetAsGeoJSON(Long id);
```

**File**: `DatasetServiceImpl.java`

实现 GeoJSON 导出（使用 JdbcTemplate）：
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
    
    // 使用 ST_AsGeoJSON 查询
    String sql = "SELECT ST_AsGeoJSON(geom) as geojson, * FROM " + tableName;
    // 组装为 FeatureCollection 返回
}
```

### Task 3: DatasetController 新增 /published 接口

**File**: `backend/src/main/java/com/gisplatform/controller/DatasetController.java`

添加：
```java
@GetMapping("/published")
@Operation(summary = "获取已发布数据集列表")
public R<List<Dataset>> listPublished() {
    List<Dataset> list = datasetService.listPublishedDatasets();
    return R.ok(list);
}
```

### Task 4: DatasetController 新增 /{id}/geojson 接口

**File**: `DatasetController.java`

添加：
```java
@GetMapping("/{id}/geojson")
@Operation(summary = "获取数据集 GeoJSON")
public R<String> getGeoJSON(@PathVariable Long id) {
    String geojson = datasetService.getDatasetAsGeoJSON(id);
    return R.ok(geojson);
}
```

---

## Frontend Tasks

### Task 5: 配置 /map 路由

**File**: `frontend/src/router/index.ts`

在 staticRoutes 中添加：
```typescript
{
  path: '/map',
  name: 'MapViewer',
  component: () => import('@/views/map/index.vue'),
  meta: { title: '地图查看', requiresAuth: true }
}
```

### Task 6: 创建地图主页面

**File**: `frontend/src/views/map/index.vue`

- 布局：左侧 LayerPanel (250px) + 右侧 MapContainer (flex: 1)
- 使用 Element Plus 的 el-container, el-aside, el-main
- 声明 emits: `['layer-change']`，透传子组件事件

### Task 7: 创建 LayerPanel 组件

**File**: `frontend/src/views/map/LayerPanel.vue`

- 组件 Props: 无
- 组件 Emits: `['layer-change']`
- 调用 `GET /api/v1/datasets/published` 获取数据
- 使用 el-checkbox-group 展示图层列表
- 勾选状态变化时 emit 'layer-change' 事件

### Task 8: 创建 MapContainer 组件

**File**: `frontend/src/views/map/MapContainer.vue`

- 组件 Props: `layers` (已选中的图层列表)
- 使用 OpenLayers 初始化地图：
  - OSM 底图
  - View 初始中心 [116.4, 39.9] (北京), zoom 10
- 监听 layers 变化，动态 addLayer/removeLayer
- 使用 Select interaction 处理要素点击
- 使用 Overlay/Popup 显示要素属性

---

## Implementation Order

1. 后端接口 (Task 1-4)
2. 前端路由 (Task 5)
3. 前端组件 (Task 6-8)

## Notes

- 后端 GeoJSON 导出需要处理坐标系转换
- 前端需要处理 API 请求的 JWT Token
- 大数据量场景需要考虑分页或限制
