# Design: fix-image-extent-final

## 修复方案

### 1. 后端强制确保 extent 为 EPSG:4326

**文件**: `backend/.../ImageServiceImpl.java`

修改 `getImageWmsInfo` 方法中的 extent 转换逻辑：

```java
// 1. 首先尝试 GeoTools 转换
double[] transformedExtent = CrsTransformUtil.transformExtentToWgs84(extent, sourceCrs);

if (transformedExtent != null) {
    // GeoTools 转换成功
    info.setExtent(transformedExtent);
    info.setCrs("EPSG:3857");
} else {
    // GeoTools 转换失败，使用简化的数学换算作为备用
    // EPSG:3857 → EPSG:4326 的简化转换
    // Web Mercator 投影公式
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:3857");
    log.warn("Using fallback extent transformation for dataset {}", id);
}

// 辅助方法：简化的 EPSG:3857 → EPSG:4326 转换
private double[] transformExtentSimple(double[] extent) {
    // 使用 Web Mercator 反投影的简化近似公式
    // x = R * (λ + π) 
    // y = R * ln(tan(π/4 + φ/2))
    // 反算: λ = x/R - π, φ = 2 * atan(exp(y/R)) - π/2
    
    double R = 6378137.0;
    double PI = Math.PI;
    
    double minX_rad = extent[0] / R;
    double maxX_rad = extent[2] / R;
    double minY_rad = Math.atan(Math.exp(extent[1] / R));
    double maxY_rad = Math.atan(Math.exp(extent[3] / R));
    
    double[] result = new double[4];
    result[0] = Math.toDegrees(minX_rad);
    result[1] = Math.toDegrees(2 * minY_rad - PI / 2);
    result[2] = Math.toDegrees(maxX_rad);
    result[3] = Math.toDegrees(2 * maxY_rad - PI / 2);
    
    return result;
}
```

### 2. 前端简化 extent 使用

**文件**: `frontend/.../MapContainer.vue`

修改 `loadImageLayer` 函数，移除所有 CRS 判断和转换逻辑：

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

  // 缩放到影像范围 - 简化版
  // 后端返回的 extent 已经是 EPSG:4326 经纬度坐标，直接使用
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    map.value.getView().fit(imageInfo.extent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
```

## 数据流（修复后）

```
后端:
├─ ImageServiceImpl.getImageWmsInfo()
│   ├─ 原始 extent = EPSG:3857 米制坐标
│   ├─ 尝试 CrsTransformUtil.transformExtentToWgs84()转换
│   │   ✓ 成功 → 返回 EPSG:4326 经纬度
│   │   ✗ 失败 → 使用 fallback 数学换算 → 返回 EPSG:4326 经纬度
│   └─ info.setExtent(经纬度坐标)
│
前端:
├─ imageInfo.extent = [lon1, lat1, lon2, lat2]  // 后端保证是经纬度
├─ map.getView().fit(imageInfo.extent)
│   ✓ 正确缩放
└─ 地图显示正确区域
```

## 验证步骤

1. Maven 编译后端
2. 前端 `npm run dev`
3. 上传并发布 GeoTIFF 影像
4. 在地图上添加影像图层
5. 验证：
   - 地图自动缩放到正确区域
   - 影像正确显示
