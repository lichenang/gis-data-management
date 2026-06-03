# Design: fix-geojson-double-parse

## Overview

在 MapContainer.vue 的 loader 回调中，对 API 响应的 data 字段进行二次 JSON 解析。

## Current Code

```typescript
// MapContainer.vue:158-164
.then(text => {
  const jsonData = JSON.parse(text)
  const features = new GeoJSON().readFeatures(jsonData, {
    featureProjection: 'EPSG:4326'
  })
  source.addFeatures(features)
  console.log('[MapContainer] Loaded features:', features.length)
})
```

## Problem

API 返回格式:
```json
{
  "code": 200,
  "data": "{\"type\":\"FeatureCollection\",\"features\":[...]}",  // 字符串
  "success": true
}
```

当前代码仅解析了一层，得到:
```javascript
{ code: 200, data: "{\"type\":\"FeatureCollection\"...}", success: true }
```

`readFeatures` 无法正确解析，返回空数组。

## Change

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

## Implementation Notes

- 使用 try-catch 包裹第二次 JSON.parse，防止 data 字段不是有效 JSON
- 日志清晰区分两次解析的错误
