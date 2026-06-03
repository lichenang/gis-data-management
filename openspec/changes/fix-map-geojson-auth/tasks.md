# Tasks: fix-map-geojson-auth

## Task 1: 修改 loadLayer 函数使用自定义 loader

**File**: `frontend/src/views/map/MapContainer.vue`

**修改 loadLayer 函数**:

将第 130-137 行：
```typescript
function loadLayer(layerInfo: LayerInfo) {
  if (!map.value) return

  const source = new VectorSource({
    url: `/api/v1/datasets/${layerInfo.id}/geojson`,
    format: new GeoJSON()
  })
```

改为：
```typescript
function loadLayer(layerInfo: LayerInfo) {
  if (!map.value) return

  const source = new VectorSource({
    loader: async (extent, resolution, projection) => {
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
      const features = new GeoJSON().readFeatures(text, {
        featureProjection: projection
      })
      source.addFeatures(features)
    },
    format: new GeoJSON()
  })
```

## Task 2: 验证

1. 重启前端服务
2. 访问地图页面
3. 勾选已发布数据集
4. 验证 geojson 请求返回 200
5. 验证地图显示要素
