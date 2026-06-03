# Design: fix-image-projection-mismatch

## 修复方案

### Task 1: 前端 TileWMS 添加 projection 参数

**文件**: `frontend/src/views/map/MapContainer.vue`

修改 `loadImageLayer` 函数，给 TileWMS 添加 projection 参数：

```typescript
function loadImageLayer(imageInfo: ImageLayerInfo) {
  if (!map.value) return

  // 添加 projection 参数，解决 WMS 请求坐标系问题
  const wmsSource = new TileWMS({
    url: imageInfo.wmsUrl,
    params: {
      'LAYERS': imageInfo.layerName,
      'TILED': true
    },
    projection: imageInfo.crs || 'EPSG:4326',  // ← 添加这行
    serverType: 'geoserver',
    transition: 0
  })

  // ... 其余代码不变

  // 缩放到影像范围 - 需要根据 crs 判断是否需要转换
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    const view = map.value.getView()
    const viewProjection = view.getProjection().getCode()  // 'EPSG:4326'
    let extent: [number, number, number, number] = imageInfo.extent

    // 如果 extent 是 EPSG:3857 但 view 是 EPSG:4326，需要转换
    if (imageInfo.crs === 'EPSG:3857' && viewProjection === 'EPSG:4326') {
      import('ol/proj').then((proj) => {
        const transformed = proj.transformExtent(
          extent,
          'EPSG:3857',
          'EPSG:4326'
        )
        view.fit(transformed, {
          padding: [50, 50, 50, 50],
          maxZoom: 15,
          duration: 500
        })
      })
      return
    }

    // 否则直接使用（extent 已是 EPSG:4326 或无 crs 信息）
    view.fit(extent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
```

### Task 2: 后端增强日志

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

修改 `getImageWmsInfo` 方法，增强日志输出：

```java
@Override
public ImageWmsInfo getImageWmsInfo(Long id) {
    Dataset dataset = this.getById(id);
    // ... 现有验证代码 ...

    ImageWmsInfo info = new ImageWmsInfo();
    info.setLayerName("raster_" + id);
    info.setOpacity(0.8);
    info.setWmsUrl(dataset.getWmsUrl());

    // 解析 extent 并转换到 EPSG:4326
    if (dataset.getExtent() != null && !dataset.getExtent().isEmpty()) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> extentMap = mapper.readValue(dataset.getExtent(), Map.class);
            double[] extent = new double[4];
            extent[0] = ((Number) extentMap.get("minX")).doubleValue();
            extent[1] = ((Number) extentMap.get("minY")).doubleValue();
            extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
            extent[3] = ((Number) extentMap.get("maxY")).doubleValue();

            String sourceCrs = dataset.getSrs();
            log.info("Image {} extent transformation: sourceCRS={}, extent=[{}, {}, {}, {}]",
                    id, sourceCrs, extent[0], extent[1], extent[2], extent[3]);

            double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

            if (transformedExtent != null) {
                info.setExtent(transformedExtent);
                info.setCrs("EPSG:4326");
                log.info("Successfully transformed extent to EPSG:4326: [{}, {}, {}, {}]",
                        transformedExtent[0], transformedExtent[1], transformedExtent[2], transformedExtent[3]);
            } else {
                info.setExtent(extent);
                info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
                log.warn("Failed to transform extent for dataset {}, using original extent. sourceCrs={}",
                        id, sourceCrs);
            }

        } catch (Exception e) {
            log.warn("Failed to parse extent for dataset {}: {}", id, e.getMessage());
        }
    }

    return info;
}
```

## 数据流（修复后）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     修复后的数据流                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  后端:                                                                  │
│  dataset.srs = "EPSG:3857"                                             │
│  dataset.extent = {minX, minY, maxX, maxY} (3857)                      │
│         │                                                               │
│         ▼                                                               │
│  getImageWmsInfo()                                                      │
│  ├─ CrsTransformUtil.transformExtentToWgs84()                         │
│  │   ✓ 转换 extent: EPSG:3857 → EPSG:4326                             │
│  └─ 返回:                                                               │
│      {                                                                  │
│        extent: [lon1, lat1, lon2, lat2],  // EPSG:4326                │
│        crs: "EPSG:4326"                                                │
│      }                                                                  │
│                                                                         │
│  前端:                                                                  │
│  imageInfo = {                                                          │
│    extent: [lon1, lat1, lon2, lat2],  // 已是 EPSG:4326              │
│    crs: "EPSG:4326"                                                    │
│  }                                                                      │
│         │                                                               │
│         ▼                                                               │
│  loadImageLayer()                                                       │
│  ├─ TileWMS(projection: "EPSG:4326")                                   │
│  │   ✓ WMS 请求用正确的坐标系                                          │
│  └─ fit(extent)                                                        │
│      ✓ extent 已是 EPSG:4326，直接 fit                                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 验证步骤

### 1. 后端验证

```bash
# 启动应用，上传并发布一个影像
# 调用 GET /api/v1/images/{id}/wms-url
# 检查日志输出：
# "Image X extent transformation: sourceCRS=EPSG:3857, extent=[...]"
# "Successfully transformed extent to EPSG:4326: [...]"
```

### 2. 前端验证

1. 打开地图，添加影像图层
2. 打开浏览器开发者工具 Network 面板
3. 找到 WMS 请求，检查 `BBOX` 参数：
   - 应该是经纬度范围（如 `-180,-90,180,90` 或特定区域）
   - 而非米制坐标（如 `-20037508,-20037508,20037508,20037508`）
4. 地图应该自动缩放到影像范围，显示影像内容
