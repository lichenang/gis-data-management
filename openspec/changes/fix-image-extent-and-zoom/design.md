# Design: fix-image-extent-and-zoom

## Technical Design

### 1. Backend Changes

#### 1.1 Dataset.extent 字段说明

`Dataset.extent` 存储格式为 JSON 字符串：
```json
{"minX": 115.5, "minY": 39.5, "maxX": 116.5, "maxY": 40.5}
```

#### 1.2 ImageWmsInfo 新增 extent 字段

**File**: `backend/src/main/java/com/gisplatform/entity/ImageWmsInfo.java`

```java
@Schema(description = "影像覆盖范围 [minX, minY, maxX, maxY]")
private double[] extent;
```

#### 1.3 publishImageDataset() 设置 extent

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

修改 `publishImageDataset()` 方法，在 `dataset.setWmsUrl()` 之后添加：

```java
// 从 raster_metadata.transform 提取 extent
String transformJson = rasterMetadata.getTransform();
if (transformJson != null && !transformJson.isEmpty()) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> transform = mapper.readValue(transformJson, Map.class);
        Double minX = ((Number) transform.get("minX")).doubleValue();
        Double minY = ((Number) transform.get("minY")).doubleValue();
        Double maxX = ((Number) transform.get("maxX")).doubleValue();
        Double maxY = ((Number) transform.get("maxY")).doubleValue();
        String extentJson = String.format("{\"minX\":%s,\"minY\":%s,\"maxX\":%s,\"maxY\":%s}",
                minX, minY, maxX, maxY);
        dataset.setExtent(extentJson);
    } catch (Exception e) {
        log.warn("Failed to parse transform for extent: {}", e.getMessage());
    }
}
```

#### 1.4 getImageWmsInfo() 返回 extent

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

修改 `getImageWmsInfo()` 方法：

```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("影像数据集不存在");
    }

    if (dataset.getWmsUrl() == null || dataset.getWmsUrl().isEmpty()) {
        throw new RuntimeException("影像数据集未发布到 GeoServer");
    }

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setCrs(dataset.getSrs() != null ? dataset.getSrs() : "EPSG:4326");
    info.setOpacity(0.8);
    info.setWmsUrl(dataset.getWmsUrl());

    // 解析 extent
    if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
            double[] extent = new double[4];
            extent[0] = ((Number) extentMap.get("minX")).doubleValue();
            extent[1] = ((Number) extentMap.get("minY")).doubleValue();
            extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
            extent[3] = ((Number) extentMap.get("maxY")).doubleValue();
            info.setExtent(extent);
        } catch (Exception e) {
            log.warn("Failed to parse extent: {}", e.getMessage());
        }
    }

    return info;
}
```

### 2. Frontend Changes

#### 2.1 ImageWmsInfo 类型更新

**File**: `frontend/src/api/image.ts`

```typescript
export interface ImageWmsInfo {
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  extent?: [number, number, number, number]  // [minX, minY, maxX, maxY]
}
```

#### 2.2 MapContainer 加载时缩放

**File**: `frontend/src/views/map/MapContainer.vue`

修改 `loadImageLayer()` 函数：

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
    opacity: imageInfo.opacity,
    properties: { layerId: imageInfo.id, type: 'image' }
  })

  map.value.addLayer(imageLayer)
  imageLayersMap.value[imageInfo.id] = {
    layer: imageLayer,
    source: wmsSource
  }

  // 缩放到影像范围
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    const extent: [number, number, number, number] = imageInfo.extent
    map.value.getView().fit(extent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
```

## Data Flow After Fix

```
┌─────────────────────────────────────────────────────────────────────┐
│                 DATA FLOW (AFTER FIX)                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  发布时 (publishImageDataset):                                       │
│  ┌─────────────────┐      ┌─────────────────────────────────┐      │
│  │ raster_metadata │      │ dataset                          │      │
│  │ .transform      │      │ .extent = {"minX":..., ...}     │ ← NEW│
│  │ = {"minX":...,  │ ──✓──│ ✓ extent parsed from transform  │      │
│  │   "maxY":...}   │      └─────────────────────────────────┘      │
│  └─────────────────┘                                                 │
│                                                                     │
│  加载时 (loadImageLayer):                                            │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │ ImageWmsInfo = { wmsUrl, layerName, extent: [minX,...] } ← extent││
│  └─────────────────────────────────────────────────────────────────┘│
│                                │                                     │
│                                ▼                                     │
│                    map.getView().fit(extent)  ← ZOOM!               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```
