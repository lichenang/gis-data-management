# Tasks: add-image-layer-to-map

## Backend Tasks

### Task 1: ImageService 新增 listPublishedImages 方法

**File**: `backend/src/main/java/com/gisplatform/service/ImageService.java`

添加方法签名：
```java
List<Dataset> listPublishedImages();
```

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

实现：
```java
@Override
public List<Dataset> listPublishedImages() {
    return this.list(new LambdaQueryWrapper<Dataset>()
            .eq(Dataset::getType, "raster")
            .eq(Dataset::getStatus, "published")
            .eq(Dataset::getDeleted, 0));
}
```

### Task 2: ImageWmsInfo 响应类

**File**: `backend/src/main/java/com/gisplatform/entity/ImageWmsInfo.java`

新建类：
```java
@Data
public class ImageWmsInfo {
    private String wmsUrl;
    private String layerName;
    private String crs;
    private Double opacity;
}
```

### Task 3: ImageService 新增 getImageWmsInfo 方法

**File**: `ImageService.java`

添加方法签名：
```java
ImageWmsInfo getImageWmsInfo(Long id);
```

**File**: `ImageServiceImpl.java`

实现：
```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    RasterMetadata metadata = rasterMetadataMapper.selectOne(
            new LambdaQueryWrapper<RasterMetadata>()
                    .eq(RasterMetadata::getDatasetId, id));

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName(dataset.getName());
    info.setCrs(dataset.getSrs() != null ? dataset.getSrs() : "EPSG:4326");
    info.setOpacity(0.8);

    // 构建 GeoServer WMS URL
    String geoserverUrl = "http://localhost:8080/geoserver"; // 从配置读取
    info.setWmsUrl(geoserverUrl + "/wms");

    return info;
}
```

### Task 4: ImageController 新增接口

**File**: `backend/src/main/java/com/gisplatform/controller/ImageController.java`

添加：
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

---

## Frontend Tasks

### Task 5: API 扩展

**File**: `frontend/src/api/image.ts`

添加：
```typescript
export interface ImageWmsInfo {
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
}

export function getPublishedImages() {
  return get<{ code: number; data: Dataset[] }>('/images/published')
}

export function getImageWmsUrl(id: number) {
  return get<{ code: number; data: ImageWmsInfo }>(`/images/${id}/wms-url`)
}
```

### Task 6: LayerPanel 添加影像图层分类

**File**: `frontend/src/views/map/LayerPanel.vue`

**新增数据结构**：
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
```

**新增状态**：
```typescript
const imageLayers = ref<ImageLayerInfo[]>([])
const selectedImageLayerIds = ref<number[]>([])
const imageLayerOpacities = ref<Record<number, number>>({})
const imageLayerWmsInfoCache = ref<Record<number, ImageWmsInfo>>({})
```

**模板修改**：
```vue
<el-collapse>
  <el-collapse-item title="矢量图层" name="vector">
    <!-- 现有矢量图层内容 -->
  </el-collapse-item>

  <el-collapse-item title="影像图层" name="image">
    <div v-if="imageLayers.length === 0" class="empty">
      <p>暂无已发布影像</p>
    </div>
    <div v-else>
      <div
        v-for="layer in imageLayers"
        :key="layer.id"
        class="image-layer-item"
      >
        <el-checkbox
          :value="layer.id"
          v-model="selectedImageLayerIds"
          @change="handleImageLayerChange"
        >
          {{ layer.name }}
        </el-checkbox>
        <el-slider
          v-if="selectedImageLayerIds.includes(layer.id)"
          v-model="imageLayerOpacities[layer.id]"
          :min="0"
          :max="100"
          :step="5"
          show-stops
          @change="handleImageLayerChange"
        />
      </div>
    </div>
  </el-collapse-item>
</el-collapse>
```

**新增方法**：
```typescript
async function fetchPublishedImageLayers() {
  loading.value = true
  try {
    const response = await getPublishedImages()
    if (response.code === 200) {
      for (const d of response.data || []) {
        const wmsResponse = await getImageWmsUrl(d.id)
        if (wmsResponse.code === 200) {
          imageLayers.value.push({
            id: d.id,
            name: d.name || '',
            ...wmsResponse.data
          })
          imageLayerOpacities.value[d.id] = 80
        }
      }
    }
  } catch (error) {
    console.error('Failed to fetch image layers:', error)
  } finally {
    loading.value = false
  }
}

function handleImageLayerChange() {
  const selected = imageLayers.value
    .filter(l => selectedImageLayerIds.value.includes(l.id))
    .map(l => ({
      ...l,
      opacity: imageLayerOpacities.value[l.id] || 80
    }))
  emit('image-layer-change', selected)
}
```

**新增 emit**：
```typescript
const emit = defineEmits<{
  (e: 'layer-change', layers: VectorLayerInfo[]): void
  (e: 'image-layer-change', layers: ImageLayerInfo[]): void
}>()
```

### Task 7: MapContainer 添加影像图层支持

**File**: `frontend/src/views/map/MapContainer.vue`

**新增导入**：
```typescript
import TileWMS from 'ol/source/TileWMS'
```

**新增 Props**：
```typescript
interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
}

const props = defineProps<{
  layers: VectorLayerInfo[]
  imageLayers: ImageLayerInfo[]
}>()
```

**新增状态**：
```typescript
const imageLayersMap = ref<Record<number, {
  layer: TileLayer
  source: TileWMS
}>>({})
```

**新增方法**：
```typescript
function loadImageLayer(imageInfo: ImageLayerInfo) {
  if (!map.value) return

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

  map.value.addLayer(imageLayer)
  imageLayersMap.value[imageInfo.id] = {
    layer: imageLayer,
    source: wmsSource
  }
}

function unloadImageLayer(imageId: number) {
  if (!map.value) return

  const entry = imageLayersMap.value[imageId]
  if (entry) {
    map.value.removeLayer(entry.layer)
    delete imageLayersMap.value[imageId]
  }
}

function updateImageOpacity(imageId: number, opacity: number) {
  const entry = imageLayersMap.value[imageId]
  if (entry) {
    entry.layer.setOpacity(opacity / 100)
  }
}
```

**新增 watch**：
```typescript
watch(() => props.imageLayers, (newLayers, oldLayers) => {
  const oldIds = new Set((oldLayers || []).map(l => l.id))
  const newIds = new Set(newLayers.map(l => l.id))

  newLayers.forEach(layer => {
    if (!(layer.id in imageLayersMap.value)) {
      loadImageLayer(layer)
    } else {
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

### Task 8: 地图页面父组件修改

**File**: `frontend/src/views/map/index.vue`

**新增状态**：
```typescript
interface ImageLayerDisplayInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
}

const selectedImageLayers = ref<ImageLayerDisplayInfo[]>([])
```

**修改模板**：
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

**新增方法**：
```typescript
function handleImageLayerChange(layers: ImageLayerDisplayInfo[]) {
  selectedImageLayers.value = layers
}
```

---

## Implementation Order

1. Backend Tasks (Task 1-4)
2. Frontend API (Task 5)
3. LayerPanel (Task 6)
4. MapContainer (Task 7)
5. 父组件修改 (Task 8)

## Notes

- GeoServer WMS URL 应从后端配置获取，避免硬编码
- 影像图层加载需要处理跨域问题（GeoServer CORS 配置）
- 透明度调节使用 OpenLayers 的 layer.setOpacity() 方法
- 影像图层默认放置在矢量图层下方（zIndex 顺序）
