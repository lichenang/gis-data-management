# Design: add-image-layer-to-map

## Technical Design

### 1. Backend Implementation

#### 1.1 ImageController 新增接口

**File**: `backend/src/main/java/com/gisplatform/controller/ImageController.java`

```java
@GetMapping("/published")
@Operation(summary = "获取已发布影像数据集列表")
public R<List<Dataset>> listPublished() {
    List<Dataset> list = imageService.listPublishedImages();
    return R.ok(list);
}

@GetMapping("/{id}/wms-url")
@Operation(summary = "获取影像图层WMS地址")
public R<ImageWmsInfo> getWmsUrl(@PathVariable Long id) {
    ImageWmsInfo wmsInfo = imageService.getImageWmsInfo(id);
    return R.ok(wmsInfo);
}
```

**ImageWmsInfo 响应结构**:
```java
public class ImageWmsInfo {
    private String wmsUrl;      // GeoServer WMS 地址
    private String layerName;   // GeoServer 图层名
    private String crs;         // 坐标系
    private Double opacity;     // 默认透明度
}
```

#### 1.2 ImageService 新增方法

**File**: `backend/src/main/java/com/gisplatform/service/ImageService.java`

```java
List<Dataset> listPublishedImages();

ImageWmsInfo getImageWmsInfo(Long id);
```

**实现逻辑**:
- `listPublishedImages()`: 查询 `dataset` 表中 `type='raster'` 且 `status='published'` 的记录
- `getImageWmsInfo()`: 根据 dataset id 查询 raster_metadata 获取 GeoServer 发布信息，构建 WMS URL

WMS URL 格式：
```
{geoserver_base_url}/geoserver/{workspace}/{layer_name}/wms?service=WMS&version=1.1.0&request=GetMap
```

### 2. Frontend Implementation

#### 2.1 API 扩展

**File**: `frontend/src/api/image.ts`

```typescript
export function getPublishedImages() {
  return get<{ code: number; data: Dataset[] }>('/images/published')
}

export interface ImageWmsInfo {
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
}

export function getImageWmsUrl(id: number) {
  return get<{ code: number; data: ImageWmsInfo }>(`/images/${id}/wms-url`)
}
```

#### 2.2 LayerPanel 修改

**File**: `frontend/src/views/map/LayerPanel.vue`

**数据结构**:
```typescript
interface VectorLayerInfo {
  id: number
  name: string
  geometryType: string
}

interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  visible: boolean
}
```

**UI 结构**:
```
┌──────────────────────────────────────┐
│ 图层列表                              │
├──────────────────────────────────────┤
│ ▼ 矢量图层 (已发布)                    │
│   ☑ 图层A (面)                        │
│   ☑ 图层B (线)                        │
├──────────────────────────────────────┤
│ ▼ 影像图层                            │
│   ☑ 影像A    [━━━○━━━] 80%           │
│   ☑ 影像B    [━━━○━━━] 60%           │
└──────────────────────────────────────┘
```

**交互逻辑**:
- 影像图层使用折叠面板（el-collapse）分组
- 每个影像图层显示复选框 + 名称 + 透明度滑块
- 透明度默认 80%，范围 0-100%
- 勾选状态和透明度变化时触发 `image-layer-change` 事件

#### 2.3 MapContainer 修改

**File**: `frontend/src/views/map/MapContainer.vue`

**新增 Props**:
```typescript
interface Props {
  layers: VectorLayerInfo[]           // 现有矢量图层
  imageLayers: ImageLayerInfo[]       // 新增影像图层
}
```

**新增方法**:
```typescript
function loadImageLayer(imageInfo: ImageLayerInfo): void {
  // 使用 TileWMS 加载影像
  const wmsSource = new TileWMS({
    url: imageInfo.wmsUrl,
    params: {
      'LAYERS': imageInfo.layerName,
      'TILED': true
    },
    serverType: 'geoserver',
    transition: 0
  })

  const imageLayer = new TileLayer({
    source: wmsSource,
    opacity: imageInfo.opacity / 100,
    properties: { layerId: imageInfo.id, type: 'image' }
  })

  map.value?.addLayer(imageLayer)
  imageLayers.value[imageInfo.id] = { layer: imageLayer, source: wmsSource }
}

function unloadImageLayer(imageId: number): void {
  const entry = imageLayers.value[imageId]
  if (entry) {
    map.value?.removeLayer(entry.layer)
    delete imageLayers.value[imageId]
  }
}

function updateImageOpacity(imageId: number, opacity: number): void {
  const entry = imageLayers.value[imageId]
  if (entry) {
    entry.layer.setOpacity(opacity / 100)
  }
}
```

**监听 imageLayers 变化**:
```typescript
watch(() => props.imageLayers, (newLayers, oldLayers) => {
  const oldIds = new Set((oldLayers || []).map(l => l.id))
  const newIds = new Set(newLayers.map(l => l.id))

  // 处理新增/删除
  newLayers.forEach(layer => {
    if (!(layer.id in imageLayers.value)) {
      loadImageLayer(layer)
    } else {
      // 更新透明度
      updateImageOpacity(layer.id, layer.opacity)
    }
  })

  oldIds.forEach(id => {
    if (!newIds.has(id)) {
      unloadImageLayer(id)
    }
  })
}, { deep: true })
```

#### 2.4 父组件修改

**File**: `frontend/src/views/map/index.vue`

**修改**:
```typescript
interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  visible: boolean
}

const selectedImageLayers = ref<ImageLayerInfo[]>([])

function handleImageLayerChange(layers: ImageLayerInfo[]) {
  selectedImageLayers.value = layers
}
```

**模板**:
```vue
<LayerPanel
  @layer-change="handleLayerChange"
  @image-layer-change="handleImageLayerChange"
/>
<MapContainer
  :layers="selectedLayers"
  :image-layers="selectedImageLayers"
/>
```

## Data Flow

### 加载影像图层流程

```
用户勾选影像图层
        │
        ▼
LayerPanel 获取影像图层 WMS URL
        │
        ▼
调用 GET /api/v1/images/{id}/wms-url
        │
        ▼
后端返回 WMS 地址信息
        │
        ▼
MapContainer 创建 TileWMS Source
        │
        ▼
创建 TileLayer 并添加到地图
        │
        ▼
影像显示在地图上
```

### 透明度调节流程

```
用户拖动透明度滑块
        │
        ▼
LayerPanel 更新 opacity 值
        │
        ▼
emit 'image-layer-change' 事件
        │
        ▼
父组件更新 selectedImageLayers
        │
        ▼
MapContainer watch 检测到变化
        │
        ▼
调用 layer.setOpacity() 更新透明度
```

## Edge Cases

1. 影像 WMS 服务不可用：显示错误提示，地图不崩溃
2. 影像坐标系与底图不一致：使用 OpenLayers 投影转换
3. 影像数据量过大：TileWMS 自动分块加载
4. 影像 URL 获取失败：显示图层但禁用勾选
5. 影像图层与矢量图层叠加顺序：影像在下，矢量在上
