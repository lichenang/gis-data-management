# Tasks: fix-map-loader-async

## Task 1: 将 loader 从 async/await 改为 Promise.then()

- [x] 完成

## Task 2: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `loadLayer` 函数中的 loader 回调:

将:
```typescript
loader: async (extent: any, resolution: any, projection: any) => {
  const token = localStorage.getItem('access_token')
  const url = `/api/v1/datasets/${layerInfo.id}/geojson`

  const response = await fetch(url, {
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  })

  if (!response.ok) {
    console.error('Failed to load GeoJSON:', response.status)
    return
  }

  const text = await response.text()

  let jsonData
  try {
    jsonData = JSON.parse(text)
  } catch (e) {
    console.error('[MapContainer] Failed to parse GeoJSON:', e)
    return
  }

  const features = new GeoJSON().readFeatures(jsonData, {
    featureProjection: 'EPSG:4326'
  })
  source.addFeatures(features)

  console.log('[MapContainer] Loaded features:', features.length)
```

改为:
```typescript
loader: (extent: any, resolution: any, projection: any) => {
  const token = localStorage.getItem('access_token')
  const url = `/api/v1/datasets/${layerInfo.id}/geojson`

  fetch(url, {
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      return response.text()
    })
    .then(text => {
      const jsonData = JSON.parse(text)
      const features = new GeoJSON().readFeatures(jsonData, {
        featureProjection: 'EPSG:4326'
      })
      source.addFeatures(features)
      console.log('[MapContainer] Loaded features:', features.length)
    })
    .catch(error => {
      console.error('[MapContainer] Failed to load GeoJSON:', error)
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
