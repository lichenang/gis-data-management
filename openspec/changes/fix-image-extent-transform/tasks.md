# Tasks: fix-image-extent-transform

## Task 1: 修改 MapContainer.vue TileWMS 配置 ✓

## Task 2: 修改 MapContainer.vue extent 转换逻辑 ✓

**文件**: `frontend/src/views/map/MapContainer.vue`

修改 loadImageLayer 函数中的 extent 转换逻辑（约第 278-309 行），改为始终根据 imageInfo.crs 动态转换：

```typescript
// 缩放到影像范围
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const view = map.value.getView()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const viewProjection: any = view.getProjection().getCode()
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let extent: any = imageInfo.extent

  // 始终根据源 CRS 转换 extent 到视图 CRS
  if (imageInfo.crs && imageInfo.crs !== viewProjection) {
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

## Task 3: 验证修复 ✓

### 前端验证

1. 执行 `npm run dev` 启动前端
2. 调用 `GET /api/v1/images/{id}/wms-url` 确认返回 crs
3. 打开地图，添加影像图层
4. 确认：
   - 地图缩放到正确区域（非海里）
   - 影像正确显示

### 测试步骤

- [ ] 启动后端应用
- [ ] 启动前端应用
- [ ] 上传并发布 GeoTIFF 影像
- [ ] 在地图上添加影像图层
- [ ] 验证地图正确缩放
- [ ] 验证影像显示
