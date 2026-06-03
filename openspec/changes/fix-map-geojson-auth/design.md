# Design: fix-map-geojson-auth

## Technical Design

### Current Code

```typescript
const source = new VectorSource({
  url: `/api/v1/datasets/${layerInfo.id}/geojson`,
  format: new GeoJSON()
})
```

### Fixed Code

```typescript
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

### Key Changes

1. 移除 `url:` 属性
2. 添加 `loader:` 函数
3. 手动添加 `Authorization` 请求头
4. 使用 `source.addFeatures()` 添加要素

## LoadLayer Function Context

loader 函数需要访问 layerInfo.id，但由于闭包作用域，需要调整 loadLayer 函数结构，确保 loader 能正确访问 layerInfo。
