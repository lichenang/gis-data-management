# Proposal: fix-geojson-double-parse

## Summary

修复 MapContainer.vue 中 GeoJSON 解析失败的问题。

## Problem

用户反映即使添加了 JSON.parse，要素数仍为 0，且控制台无报错。

### Root Cause

GeoJSON API 返回的响应格式是:

```json
{
  "code": 200,
  "data": "{\"type\":\"FeatureCollection\",\"features\":[...]}",  // data 是字符串!
  "success": true
}
```

当前代码只对响应体做了一次 `JSON.parse()`:

```typescript
const jsonData = JSON.parse(text)  // 得到 { code: 200, data: "...", success: true }
const features = new GeoJSON().readFeatures(jsonData, {...})
```

`readFeatures` 收到的是 `{ code, data, success }` 对象，不是 FeatureCollection，所以返回空数组。

需要对 `jsonData.data` 再做一次 `JSON.parse()`:

```typescript
const jsonData = JSON.parse(text)  // { code, data: "...", success }
const geojson = JSON.parse(jsonData.data)  // FeatureCollection
```

## Solution

对 API 响应的 `data` 字段进行二次解析:

```typescript
.then(text => {
  const jsonData = JSON.parse(text)  // 第一次解析: { code, data: "..." }
  const geojson = JSON.parse(jsonData.data)  // 第二次解析: FeatureCollection
  const features = new GeoJSON().readFeatures(geojson, {...})
  // ...
})
```

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Function: `loadLayer` (loader callback)

## Success Criteria

- [ ] source.getFeatures().length > 0
- [ ] 矢量图层在地图上正确显示
- [ ] 控制台显示 Loaded features: N (N > 0)
