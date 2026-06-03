# Tasks: fix-geojson-double-parse

## Task 1: 二次解析 GeoJSON data 字段

- [x] 完成

## Task 2: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** loader 回调中的 `.then(text => {...})` 部分:

将:
```typescript
.then(text => {
  const jsonData = JSON.parse(text)
  const features = new GeoJSON().readFeatures(jsonData, {
    featureProjection: 'EPSG:4326'
  })
  source.addFeatures(features)
  console.log('[MapContainer] Loaded features:', features.length)
})
```

改为:
```typescript
.then(text => {
  const jsonData = JSON.parse(text)

  let geojson
  try {
    geojson = JSON.parse(jsonData.data)
  } catch (e) {
    console.error('[MapContainer] Failed to parse GeoJSON data:', e)
    return
  }

  const features = new GeoJSON().readFeatures(geojson, {
    featureProjection: 'EPSG:4326'
  })
  source.addFeatures(features)
  console.log('[MapContainer] Loaded features:', features.length)
})
```

## Task 2: 验证

1. 启动前端开发服务器 (`npm run dev`)
2. 打开浏览器开发者工具
3. 访问地图页面 `/map`
4. 勾选一个已发布的数据集
5. 验证:
   - 控制台显示 `[MapContainer] Loaded features:` 且数量 > 0
   - 地图上显示矢量要素
   - 执行 `source.getFeatures().length` 返回 > 0
