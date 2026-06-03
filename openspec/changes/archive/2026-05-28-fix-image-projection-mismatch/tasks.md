# Tasks: fix-image-projection-mismatch

## Task 1: 前端 TileWMS 添加 projection 参数 ✓

## Task 2: 前端处理 extent 转换逻辑 ✓

**文件**: `frontend/src/views/map/MapContainer.vue`

修改影像图层加载后的 `fit(extent)` 逻辑（约第 277-285 行），根据 crs 判断是否需要转换：

```typescript
// 缩放到影像范围
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const view = map.value.getView()
  const viewProjection = view.getProjection().getCode()
  let extent: [number, number, number, number] = imageInfo.extent

  // 如果 extent 是 EPSG:3857 但 view 是 EPSG:4326，需要转换
  if (imageInfo.crs === 'EPSG:3857' && viewProjection === 'EPSG:4326') {
    const { transformExtent } = await import('ol/proj')
    const transformed = transformExtent(extent, 'EPSG:3857', 'EPSG:4326')
    view.fit(transformed, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
    return
  }

  // 否则直接使用
  view.fit(extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

**注意**: 需要添加类型声明或使用 `any` 类型来避免 TS 错误。

## Task 3: 后端增强日志输出 ✓

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

在 `getImageWmsInfo` 方法（约第 390-418 行）中添加详细日志：

1. 在调用 CrsTransformUtil 之前记录原始 extent 和 sourceCRS
2. 在转换成功后记录转换后的 extent
3. 在转换失败时记录警告信息

```java
// 在转换前添加日志
log.info("Image {} extent transformation: sourceCRS={}, extent=[{}, {}, {}, {}]",
        id, sourceCrs, extent[0], extent[1], extent[2], extent[3]);

// 在转换成功后添加日志
log.info("Successfully transformed extent to EPSG:4326: [{}, {}, {}, {}]",
        transformedExtent[0], transformedExtent[1], transformedExtent[2], transformedExtent[3]);

// 在转换失败时记录
log.warn("Failed to transform extent for dataset {}, using original extent. sourceCrs={}",
        id, sourceCrs);
```

## Task 4: 验证修复 ✓

### 后端验证

1. 重启后端应用
2. 调用 `GET /api/v1/images/{id}/wms-url`
3. 检查日志输出，确认 extent 转换成功

### 前端验证

1. 重启前端应用
2. 打开地图，添加影像图层
3. 打开浏览器开发者工具，查看 WMS 请求的 BBOX 参数
4. 确认：
   - BBOX 为经纬度格式（如 `-180,-90,180,90` 或区域坐标）
   - 地图自动缩放到影像范围并显示内容

### 手动测试清单

- [ ] 上传一个新的 GeoTIFF 影像
- [ ] 发布影像到 GeoServer
- [ ] 在地图上添加该影像图层
- [ ] 验证 WMS 请求使用正确的坐标系
- [ ] 验证地图正确缩放到影像范围
- [ ] 验证影像正确显示（不是空白）
