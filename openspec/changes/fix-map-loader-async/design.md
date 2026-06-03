# Design: fix-map-loader-async

## Overview

在 MapContainer.vue 的 loader 回调中，将 async/await 改为 Promise.then() 链式调用。

## Current Code

```typescript
// MapContainer.vue:144-194
loader: async (extent: any, resolution: any, projection: any) => {
  const token = localStorage.getItem('access_token')
  const url = `/api/v1/datasets/${layerInfo.id}/geojson`

  const response = await fetch(url, {
    headers: { 'Authorization': token ? `Bearer ${token}` : '' }
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
  // ...
}
```

## Problem

使用 `async` 函数的 OpenLayers VectorSource loader 不会被正确等待，features 数据被静默忽略。

## Change

```typescript
loader: (extent: any, resolution: any, projection: any) => {
  const token = localStorage.getItem('access_token')
  const url = `/api/v1/datasets/${layerInfo.id}/geojson`

  fetch(url, {
    headers: { 'Authorization': token ? `Bearer ${token}` : '' }
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
}
```

## Implementation Notes

- 移除 `async` 关键字
- 使用 `.then()` 链式调用替代 `await`
- 使用 `.catch()` 统一错误处理
- 所有逻辑保持在 `.then()` 回调中执行
