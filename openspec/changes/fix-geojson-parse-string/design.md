# Design: fix-geojson-parse-string

## Overview

在 MapContainer.vue 的 loader 回调中添加 JSON.parse() 解析步骤。

## Current Code

```typescript
// MapContainer.vue:151-157
const text = await response.text()
const features = new GeoJSON().readFeatures(text, {
  featureProjection: 'EPSG:4326'
})
source.addFeatures(features)
```

## Problem

当 API 返回以下格式时会失败:

```json
// HTTP Response Body (是字符串!)
"{\"type\":\"FeatureCollection\",\"features\":[...]}"
```

`GeoJSON().readFeatures()` 期望接收:
- 字符串：有效的 GeoJSON JSON 字符串(不是字符串包装的JSON)
- 对象：已经解析的 JavaScript 对象

如果是双重转义的字符串，会导致解析失败，`features` 数组为空。

## Change

```typescript
const text = await response.text()
const jsonData = JSON.parse(text)  // 解析 JSON 字符串为对象
const features = new GeoJSON().readFeatures(jsonData, {
  featureProjection: 'EPSG:4326'
})
source.addFeatures(features)

// 添加调试日志
console.log('[MapContainer] Loaded features:', features.length)
```

## Implementation Notes

- 需要 try-catch 包装 JSON.parse，防止返回非 JSON 格式导致报错
- 添加 features.length 日志便于调试
