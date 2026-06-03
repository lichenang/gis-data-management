# 实现任务：修复 TileWMS CRS 参数问题

## 任务列表

- [x] ### 1. 修改 `loadImageLayer` 函数

**文件**: `frontend/src/views/map/MapContainer.vue`
**位置**: 第 252-287 行

**修改内容**:

在 `loadImageLayer` 函数中，修改 TileWMS 源的创建代码：

```javascript
// 修改后
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'CRS': imageInfo.crs || 'EPSG:4326'
  },
  projection: imageInfo.crs || 'EPSG:4326',
  serverType: 'geoserver',
  transition: 0
})

// ✅ 已实现
// - 添加了 'CRS': imageInfo.crs || 'EPSG:4326'
// - 移除了 'TILED': true
```

**具体变更**:
1. 在 `params` 中添加 `'CRS': imageInfo.crs || 'EPSG:4326'`
2. 移除 `params` 中的 `'TILED': true`（因为 TILED: true 时 GeoServer 会忽略 CRS 参数，使用网格集配置）

- [x] ### 2. 验证修改

1. 确保 `projection: imageInfo.crs || 'EPSG:4326'` 已设置（已存在）
2. 确保 `serverType: 'geoserver'` 已设置（已存在）
3. 确保 `params.CRS` 已显式指定
4. 确保 `params.TILED` 已移除

- [x] ### 3. 测试

修改完成后，使用 EPSG:4326 坐标系测试影像图层显示是否正常。
