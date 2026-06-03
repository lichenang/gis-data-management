# Proposal: fix-map-loader-async

## Summary

修复 MapContainer.vue 中 VectorSource loader 使用 async/await 导致要素数始终为 0 的问题。

## Problem

用户在地图页面勾选图层后，即使添加了 JSON.parse，GeoJSON 数据的要素数仍为 0。调用 `source.getFeatures().length` 返回 0。

### Root Cause (from `specs/debug-loader-features-zero.md`)

VectorSource 的 loader 使用了 `async` 函数:

```typescript
loader: async (extent, resolution, projection) => {
  await fetch(...)  // async 函数
  source.addFeatures(features)  // OpenLayers 不会等待 Promise 完成
}
```

当 loader 是 `async` 函数时:
- OpenLayers 期望 loader 同步执行或使用 callback/return 方式
- async 函数返回 Promise，OpenLayers 不会等待 Promise 完成
- 异步代码执行完成后，features 虽被添加到 source，但 OpenLayers 已忽略

## Solution

将 loader 从 async/await 改为 Promise.then() 链式调用:

```typescript
loader: (extent, resolution, projection) => {
  fetch(url).then(response => response.text()).then(text => {
    const jsonData = JSON.parse(text)
    const features = new GeoJSON().readFeatures(jsonData, {
      featureProjection: 'EPSG:4326'
    })
    source.addFeatures(features)
  }).catch(error => {...})
}
```

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Function: `loadLayer` (loader callback)

## Success Criteria

- [ ] source.getFeatures().length > 0
- [ ] 矢量图层在地图上正确显示
- [ ] console.log 显示 Loaded features: N (N > 0)
