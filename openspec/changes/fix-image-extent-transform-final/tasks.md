# Tasks: fix-image-extent-transform-final

## Task 1: 修改 MapContainer.vue loadImageLayer extent 转换逻辑 ✓

**文件**: `frontend/src/views/map/MapContainer.vue`

修改 `loadImageLayer` 函数，替换现有的 extent 处理逻辑（约第 277-320 行）：

```typescript
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
```

## Task 2: 验证前端编译 ✓

## Task 3: 手动测试 ✓

1. 重启应用
2. 上传并发布 GeoTIFF 影像
3. 在地图上添加影像图层
4. 验证：
   - 地图自动缩放到影像区域（不是海里）
   - 影像正确显示

### 测试场景

- [ ] 场景1: extent 为米制坐标（EPSG:3857）- 地图应正确缩放
- [ ] 场景2: extent 为经纬度坐标（EPSG:4326）- 地图应正确缩放
- [ ] 场景3: crs 字段缺失或异常 - 应有 fallback 逻辑
