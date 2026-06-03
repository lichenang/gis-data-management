# 设计方案：修复 TileWMS CRS 参数问题

## 修改方案

### 1. 检查并添加 CRS 参数

在 TileWMS 的 `params` 中显式指定：

```javascript
params: {
  LAYERS: 'workspace:layerName',
  CRS: 'EPSG:4326',  // 显式指定坐标系
  // ... 其他参数
}
```

### 2. 处理 TILED 参数

当 `TILED: true` 时，GeoServer 会忽略 `CRS` 参数。需要调整为：

```javascript
// 移除 TILED 参数，改用 serverType
serverType: 'geoserver',
params: {
  LAYERS: 'workspace:layerName',
  CRS: 'EPSG:4326',
}
```

### 3. 设置 projection

确保 layer 配置中包含：

```javascript
projection: 'EPSG:4326'
```

## 修改位置

在地图初始化或图层创建的相关代码中，检查 TileWMS 图层配置并应用上述修改。
