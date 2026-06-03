# Proposal: fix-geojson-parse-string

## Summary

修复 MapContainer.vue 中 GeoJSON 解析失败导致要素数为 0 的问题。

## Problem

用户在地图页面勾选图层后，API 返回了 GeoJSON 数据（Network 返回 200），但地图上没有显示任何要素。

调用 `source.getFeatures().length` 返回 0，`source.getExtent()` 返回 `[Infinity, Infinity, -Infinity, -Infinity]`。

### Root Cause

GeoJSON 接口返回的 `data` 字段是**字符串**格式（如 `"{\"type\":\"FeatureCollection\",\"features\":[...]}"`），但 loader 中直接将其传给了 `GeoJSON().readFeatures()`，需要先用 `JSON.parse()` 解析为对象。

## Solution

在 loader 中添加 `JSON.parse()` 解析步骤:

```typescript
const text = await response.text()
const jsonData = JSON.parse(text)  // 新增：解析 JSON 字符串
const features = new GeoJSON().readFeatures(jsonData, {
  featureProjection: 'EPSG:4326'
})
```

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Function: `loadLayer` (loader callback)

## Success Criteria

- [ ] GeoJSON 解析成功，要素数量 > 0
- [ ] 矢量图层在地图上正确显示
- [ ] map.getAllLayers() 返回 2+ (底图 + 矢量图层)
