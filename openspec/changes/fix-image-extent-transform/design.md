# Design: fix-image-extent-transform

## 修复方案

### 问题分析

当前前端代码假设：
- 如果 `crs === 'EPSG:3857'`，需要转换 extent

但存在边界情况：
1. 如果后端成功转换了 extent 到 EPSG:4326，前端又转换一次，会导致错误
2. 如果后端转换失败，extent 仍是 EPSG:3857，前端需要转换

**解决方案：前端始终根据 imageInfo.crs 动态转换 extent，不依赖后端转换结果**

### 修改 loadImageLayer 函数

**文件**: `frontend/src/views/map/MapContainer.vue`

修改 extent 转换逻辑，使其更加健壮：

```typescript
// 缩放到影像范围
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const view = map.value.getView()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const viewProjection: any = view.getProjection().getCode()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let extent: any = imageInfo.extent

  // 始终根据源 CRS 转换 extent 到视图 CRS
  // 这样不管后端是否成功转换，我们都能正确显示
  if (imageInfo.crs && imageInfo.crs !== viewProjection) {
    // CRS 不匹配，需要转换
    if (imageInfo.crs === 'EPSG:3857' && viewProjection === 'EPSG:4326') {
      // 从 3857 (米制) 转换到 4326 (经纬度)
      const { transformExtent } = await import('ol/proj')
      const transformed = transformExtent(extent, 'EPSG:3857', 'EPSG:4326')
      view.fit(transformed, {
        padding: [50, 50, 50, 50],
        maxZoom: 15,
        duration: 500
      })
      return
    } else if (imageInfo.crs.startsWith('EPSG:') && viewProjection.startsWith('EPSG:')) {
      // 通用CRS转换
      const { transformExtent } = await import('ol/proj')
      const transformed = transformExtent(extent, imageInfo.crs, viewProjection)
      view.fit(transformed, {
        padding: [50, 50, 50, 50],
        maxZoom: 15,
        duration: 500
      })
      return
    }
  }

  // CRS 相同或无CRS，直接使用
  view.fit(extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

### TileWMS projection 处理

**重要**：移除 TileWMS 的 projection 参数，让 OpenLayers 使用视图的 CRS 自动处理 WMS 请求。

GeoServer 能够正确响应不同 CRS 的请求，OpenLayers 会自动处理坐标转换。

```typescript
// 修改前
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: { 'LAYERS': imageInfo.layerName, 'TILED': true },
  projection: imageInfo.crs || 'EPSG:4326',  // 删除这行
  serverType: 'geoserver',
  transition: 0
})

// 修改后
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: { 'LAYERS': imageInfo.layerName, 'TILED': true },
  // 不指定 projection，让 OpenLayers 使用视图 CRS
  serverType: 'geoserver',
  transition: 0
})
```

## 数据流（修复后）

```
后端:
├─ dataset.srs = "EPSG:3857"
├─ dataset.extent = [x1,y1,x2,y2] (EPSG:3857)
├─ getImageWmsInfo():
│   ├─ 尝试转换 extent → EPSG:4326 (可能成功可能失败)
│   └─ 返回: { extent: ?, crs: "EPSG:3857" }

前端 (无论后端转换成功与否):
├─ imageInfo.extent = 后端返回的值 (可能是 3857 或 4326)
├─ imageInfo.crs = "EPSG:3857"
├─ loadImageLayer():
│   ├─ 不指定 TileWMS projection (使用视图 CRS)
│   └─ 根据 imageInfo.crs 动态转换 extent:
│       - crs === 'EPSG:3857' && view === 'EPSG:4326'
│       - → 转换 extent 到 4326
│   └─ fit(转换后的 extent)
│
└─ 结果: 地图正确缩放到影像区域 ✓
```

## 验证步骤

1. 执行 `npm run dev` 启动前端
2. 调用 `GET /api/v1/images/{id}/wms-url` 检查 crs 值
3. 在地图上添加影像图层
4. 确认：
   - 地图自动缩放到影像区域（不是海里）
   - WMS 请求使用正确的 BBOX
   - 影像正确显示
