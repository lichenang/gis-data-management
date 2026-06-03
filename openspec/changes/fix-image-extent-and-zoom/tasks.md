# Tasks: fix-image-extent-and-zoom

## Backend Tasks

### Task 1: ImageWmsInfo 新增 extent 字段

**File**: `backend/src/main/java/com/gisplatform/entity/ImageWmsInfo.java`

在类中添加：
```java
@Schema(description = "影像覆盖范围 [minX, minY, maxX, maxY]")
private double[] extent;
```

### Task 2: publishImageDataset() 设置 dataset.extent

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

在 `publishImageDataset()` 方法中，找到：
```java
dataset.setWmsUrl(layerService.getWmsUrl(workspace, layerName));
dataset.setWmtsUrl(layerService.getWmtsUrl(workspace, layerName));
```

在其后添加extent解析代码：
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

### Task 3: getImageWmsInfo() 返回 extent

**File**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

修改 `getImageWmsInfo()` 方法，在 `info.setWmsUrl(dataset.getWmsUrl())` 之后添加：

```java
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
```

---

## Frontend Tasks

### Task 4: 更新 ImageWmsInfo 类型定义

**File**: `frontend/src/api/image.ts`

修改 `ImageWmsInfo` 接口：
```typescript
export interface ImageWmsInfo {
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  extent?: [number, number, number, number]
}
```

### Task 5: MapContainer 加载影像时缩放

**File**: `frontend/src/views/map/MapContainer.vue`

修改 `loadImageLayer()` 函数，在创建并添加 layer 后添加：

```typescript
  // 缩放到影像范围
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    const extent: [number, number, number, number] = imageInfo.extent
    map.value.getView().fit(extent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
```

---

## Implementation Order

1. Backend Tasks (1-3)
2. Frontend Tasks (4-5)

## Notes

- 已发布的数据集没有 extent 信息，需要重新发布才能生效
- extent 格式为 [minX, minY, maxX, maxY]，对应 EPSG:4326 经纬度坐标
- 前端 fit() 动画时长 500ms，最大缩放级别 15
- 缩放时留 50 像素 padding 避免图层贴边
