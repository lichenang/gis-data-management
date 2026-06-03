# 地图查看模块设计方案

## 1. 概述

为 GIS Platform 实现地图查看功能，支持用户浏览已发布的空间数据集。用户在左侧面板查看图层列表并勾选加载/卸载，地图区域展示 GeoJSON 矢量数据，支持点击要素查看属性信息。

## 2. 现状分析

### 2.1 前端现状

| 组件/依赖 | 状态 | 说明 |
|----------|------|------|
| OpenLayers | ✓ 已安装 | `ol: ^10.0.0` |
| ol-mapbox-style | ✓ 已安装 | 用于 Mapbox Style 样式 |
| geotiff | ✓ 已安装 | 栅格数据支持 |
| 菜单入口 | ✓ 已存在 | `index="/map"` 已配置 |
| MapView.vue | ✗ 不存在 | 需创建 |
| 路由配置 | ✗ 缺失 | `/map` 未配置 |

### 2.2 后端现状

| 接口 | 状态 | 说明 |
|------|------|------|
| GET /api/v1/datasets | ✓ 已支持 | 分页查询所有数据集 |
| GET /api/v1/datasets/{id} | ✓ 已支持 | 获取数据集详情 |
| POST /api/v1/datasets/import | ✓ 已支持 | GeoJSON 导入 |
| GET /api/v1/datasets/{id}/geojson | ✗ 不存在 | **需新增** |
| GET /api/v1/datasets/published | ✗ 不存在 | **需新增** |

## 3. 技术架构

### 3.1 整体架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                         前端 (Vue 3 + OpenLayers)                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌────────────────┐              ┌──────────────────────────┐      │
│  │  LayerPanel    │◄────────────►│      MapContainer        │      │
│  │  (图层列表)     │              │      (地图容器)          │      │
│  │                │              │                          │      │
│  │  □ 数据集 A    │              │  ┌────────────────────┐  │      │
│  │  ☑ 数据集 B    │              │  │   OSM Tile Layer   │  │      │
│  │  □ 数据集 C    │              │  │                    │  │      │
│  │                │              │  │  ┌──────────────┐  │  │      │
│  └────────────────┘              │  │  │ GeoJSON      │  │  │      │
│                                   │  │  │ Vector Layer │  │  │      │
│                                   │  │  └──────────────┘  │  │      │
│                                   │  └────────────────────┘  │      │
│                                   └──────────────────────────┘      │
└──────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP JSON
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         后端 (Spring Boot)                            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────────┐        ┌──────────────────────────────┐   │
│  │ DatasetController   │        │ DatasetService               │   │
│  │                     │        │                               │   │
│  │ GET /datasets       │───────►│ listDatasets()               │   │
│  │ GET /datasets/{id}  │        │ getDatasetById()             │   │
│  │ **NEW** GET         │        │ **NEW** getPublishedDatasets │   │
│  │      /datasets/published     │ **NEW** getDatasetAsGeoJSON  │   │
│  │ **NEW** GET                           │                        │
│  │      /datasets/{id}/geojson          │                        │
│  └─────────────────────┘        └──────────────────────────────┘   │
│                                      │                               │
│                                      ▼                               │
│                            ┌──────────────────────────────┐        │
│                    ┌───────┤   GeoTools / PostGIS         │        │
│                    │       │   ST_AsGeoJSON               │        │
│                    │       └──────────────────────────────┘        │
│                    │                                             │
│                    ▼                                             │
│            ┌───────────────┐                                      │
│            │  PostgreSQL   │                                      │
│            │   + PostGIS   │                                      │
│            └───────────────┘                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 页面布局

```
┌─────────────────────────────────────────────────────────────────────┐
│  Header: GIS Platform                              用户头像 ▼       │
├────────────┬────────────────────────────────────────────────────────┤
│            │                                                        │
│  图层列表   │                    地图区域                           │
│            │                                                        │
│ ┌────────┐ │  ┌──────────────────────────────────────────────────┐ │
│ │☑ 行政区划│ │  │                                                  │ │
│ │  面  │  │  │            OpenLayers Map                         │ │
│ │       │  │  │                                                  │ │
│ │□ 道路  │  │  │   默认底图: OSM                                  │ │
│ │  线   │  │  │                                                  │ │
│ │       │  │  │   已加载矢量图层:                                  │ │
│ │□  POI │  │  │   - 行政区划 (GeoJSON)                           │ │
│ │  点   │  │  │                                                  │ │
│ └────────┘ │  │                                                  │ │
│            │  │                                                  │ │
│ 已发布: 5  │  └──────────────────────────────────────────────────┘ │
│            │                                                        │
│            │  ┌────────────────────────────────────────────────┐   │
│            │  │ 要素Popup: 点击要素后显示属性信息               │   │
│            │  │ 名称: 北京市                                    │   │
│            │  │ 面积: 16410 km²                                 │   │
│            │  └────────────────────────────────────────────────┘   │
└────────────┴────────────────────────────────────────────────────────┘
```

## 4. 功能需求

### 4.1 图层列表

- 显示所有 status='published' 的数据集
- 每个图层显示：名称、几何类型（点/线/面）、颜色标识
- 勾选框：勾选表示加载到地图，取消勾选表示从地图移除
- 勾选状态变更时触发地图图层增删

### 4.2 地图展示

- 底图：OpenStreetMap (OSM)
- 矢量数据：GeoJSON 格式叠加
- 支持缩放、拖拽、平移等交互
- 点击要素弹出属性信息 Popup

### 4.3 GeoJSON 接口

```
GET /api/v1/datasets/{id}/geojson

Response:
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "id": 1,
        "name": "要素名称",
        "...": "其他属性"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[x1,y1], [x2,y2], ...]]
      }
    }
  ]
}
```

### 4.4 已发布数据集列表接口

```
GET /api/v1/datasets/published

Response:
{
  "records": [
    {
      "id": 1,
      "name": "行政区划",
      "geometryType": "Polygon",
      "storageType": "postgis",
      "tableName": "dataset_1"
    }
  ]
}
```

## 5. 实现方案

### 5.1 后端实现

#### 5.1.1 新增接口

**DatasetController.java** 新增：

```java
@GetMapping("/published")
@Operation(summary = "获取已发布数据集列表", description = "返回所有 status=published 的数据集")
public R<List<Dataset>> listPublished();

@GetMapping("/{id}/geojson")
@Operation(summary = "获取数据集 GeoJSON", description = "将指定数据集导出为 GeoJSON 格式")
public R<String> getGeoJSON(@PathVariable Long id);
```

**DatasetService.java** 新增方法签名：

```java
List<Dataset> listPublishedDatasets();
String getDatasetAsGeoJSON(Long id);
```

#### 5.1.2 GeoJSON 导出实现

使用 PostGIS 的 `ST_AsGeoJSON` 函数将空间数据转换为 GeoJSON：

```sql
SELECT ST_AsGeoJSON(geom) as geojson, * FROM dataset_table WHERE id = ?
```

### 5.2 前端实现

#### 5.2.1 路由配置

**router/index.ts** 新增：

```typescript
{
  path: '/map',
  name: 'MapViewer',
  component: () => import('@/views/map/index.vue'),
  meta: { title: '地图查看', requiresAuth: true }
}
```

#### 5.2.2 组件结构

```
frontend/src/views/map/
├── index.vue          # 地图查看主页面
├── LayerPanel.vue     # 左侧图层列表组件
└── MapContainer.vue   # 地图容器组件（包含 OpenLayers 逻辑）
```

#### 5.2.3 MapContainer 核心逻辑

```typescript
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import OSM from 'ol/source/OSM'
import VectorSource from 'ol/source/Vector'
import GeoJSON from 'ol/format/GeoJSON'

// 创建地图
const map = new Map({
  layers: [
    new TileLayer({ source: new OSM() })  // 底图
  ],
  view: new View({
    center: [116.4, 39.9],  // 北京
    zoom: 10
  })
})

// 加载 GeoJSON 图层
function addGeoJSONLayer(datasetId: number, name: string) {
  const source = new VectorSource({
    url: `/api/v1/datasets/${datasetId}/geojson`,
    format: new GeoJSON()
  })
  const layer = new VectorLayer({ source, name })
  map.addLayer(layer)
}
```

### 5.3 要素点击 Popup

使用 OpenLayers 的 `ol/interaction/Select` 配合 Popup：

```typescript
import Select from 'ol/interaction/Select'

const select = new Select()
map.addInteraction(select)

select.on('select', (e) => {
  const feature = e.target.getFeatures().item(0)
  if (feature) {
    const props = feature.getProperties()
    showPopup(props)  // 显示属性信息
  }
})
```

## 6. API 设计

### 6.1 获取已发布数据集列表

```
GET /api/v1/datasets/published

Query Parameters: 无

Response:
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "北京市行政区划",
      "geometryType": "Polygon",
      "srs": "EPSG:4326",
      "description": "北京市行政区划数据"
    }
  ]
}
```

### 6.2 获取数据集 GeoJSON

```
GET /api/v1/datasets/{id}/geojson

Path Parameters:
  - id: 数据集 ID

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "type": "FeatureCollection",
    "features": [...]
  }
}
```

## 7. 数据库设计

无需新增表，使用现有 `dataset` 表的 `status` 字段筛选已发布数据集。

## 8. 任务拆分

### 后端任务
1. [ ] DatasetService 新增 listPublishedDatasets() 方法
2. [ ] DatasetService 新增 getDatasetAsGeoJSON(id) 方法
3. [ ] DatasetController 新增 /published 接口
4. [ ] DatasetController 新增 /{id}/geojson 接口

### 前端任务
1. [ ] router/index.ts 新增 /map 路由
2. [ ] 创建 views/map/index.vue 主页面
3. [ ] 创建 views/map/LayerPanel.vue 图层列表组件
4. [ ] 创建 views/map/MapContainer.vue 地图组件
5. [ ] 实现图层勾选加载/卸载逻辑
6. [ ] 实现要素点击 Popup

## 9. 风险与注意事项

1. **大数据量**：GeoJSON 导出需要考虑分页或限制返回数量
2. **坐标系转换**：数据可能是其他 EPSG 编码，需统一转换为 EPSG:4326
3. **性能优化**：大量要素时考虑使用矢量切片（MVT）
4. **样式配置**：可为不同几何类型配置不同渲染样式

## 10. 验收标准

- [ ] 前端路由 /map 正常访问
- [ ] 左侧显示已发布数据集列表
- [ ] 勾选数据集后地图显示对应 GeoJSON 图层
- [ ] 点击地图要素弹出属性信息
- [ ] 底图显示 OSM
- [ ] 后端接口返回正确的 GeoJSON 数据
