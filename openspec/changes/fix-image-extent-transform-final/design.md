# Design: fix-image-extent-transform-final

## 修复方案

### 1. 后端确认

**文件**: `backend/.../ImageServiceImpl.java`

当前代码已经修复（fix-image-crs-return-value）：
- 返回 `info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326")`
- 保持 extent 转换逻辑

无需额外修改。

### 2. 前端增强 extent 转换逻辑

**文件**: `frontend/src/views/map/MapContainer.vue`

修改 `loadImageLayer` 函数，替换现有的 extent 转换逻辑：

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

  // 缩放到影像范围 - 增强容错处理
  if (imageInfo.extent && imageInfo.extent.length === 4) {
    const view = map.value.getView()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const viewProjection: any = view.getProjection().getCode()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const extent: any = imageInfo.extent

    // 判断 extent 是否看起来像 EPSG:4326 (经纬度范围约 -180~180)
    const isGeographicExtent = 
      extent[0] >= -180 && extent[0] <= 180 &&
      extent[1] >= -90 && extent[1] <= 90 &&
      extent[2] >= -180 && extent[2] <= 180 &&
      extent[3] >= -90 && extent[3] <= 90

    // 判断 view 是否为 EPSG:4326
    const isView4326 = viewProjection === 'EPSG:4326'

    // 获取源 CRS（默认为 EPSG:3857）
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let sourceCrs: any = imageInfo.crs || 'EPSG:3857'

    // 如果 crs 不是标准 EPSG 格式，强制假设为 3857
    if (!sourceCrs.startsWith('EPSG:')) {
      sourceCrs = 'EPSG:3857'
    }

    // 关键决策逻辑
    if (isView4326 && !isGeographicExtent) {
      // view 是 EPSG:4326 但 extent 不是经纬度范围
      // → 说明 extent 是米制坐标（如 EPSG:3857），需要转换
      import('ol/proj').then((proj: any) => {
        const transformed = proj.transformExtent(extent, sourceCrs, 'EPSG:4326')
        view.fit(transformed, {
          padding: [50, 50, 50, 50],
          maxZoom: 15,
          duration: 500
        })
      })
    } else {
      // extent 看起来像 EPSG:4326 或 view 不是 EPSG:4326
      // → 直接使用 extent
      view.fit(extent, {
        padding: [50, 50, 50, 50],
        maxZoom: 15,
        duration: 500
      })
    }
  }
}
```

## 数据流（修复后）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     修复后的数据流                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  场景 1: extent 是米制坐标 (EPSG:3857)                                      │
│  ──────────────────────────────────────────                                │
│  extent = [12930000, 4855000, 12940000, 4856000]                           │
│  ↓                                                                           │
│  isGeographicExtent = false (超出 -180~180 范围)                           │
│  isView4326 = true                                                          │
│  ↓                                                                           │
│  transformExtent(extent, 'EPSG:3857', 'EPSG:4326')                         │
│  ↓                                                                           │
│  fit(转换后的经纬度坐标) ← ✓ 正确                                          │
│                                                                             │
│  场景 2: extent 已经是经纬度 (EPSG:4326)                                    │
│  ──────────────────────────────────────────                                │
│  extent = [116.3, 39.8, 116.5, 40.0]                                       │
│  ↓                                                                           │
│  isGeographicExtent = true (在 -180~180 范围内)                            │
│  isView4326 = true                                                          │
│  ↓                                                                           │
│  不转换，直接 fit(extent) ← ✓ 正确                                         │
│                                                                             │
│  场景 3: crs 为空或非标准格式                                                │
│  ────────────────────────────────                                          │
│  imageInfo.crs = undefined 或 "EPSG:WGS 84 / Pseudo-Mercator"             │
│  ↓                                                                           │
│  sourceCrs = 'EPSG:3857' (fallback)                                        │
│  ↓                                                                           │
│  extent 数值判断 → 决定是否转换                                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 验证步骤

1. Maven 编译后端
2. 启动前端开发服务器
3. 测试不同场景：
   - 影像 extent 为米制坐标
   - 影像 extent 为经纬度坐标
   - crs 字段缺失或异常

4. 确认地图正确缩放到影像区域
