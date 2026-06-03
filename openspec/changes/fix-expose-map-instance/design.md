# Design: fix-expose-map-instance

## Overview

在 MapContainer.vue 的 `initMap()` 函数中，map 实例创建后立即将其暴露到全局窗口对象。

## Current Code

```typescript
// MapContainer.vue:83-91
map.value = new Map({
  target: mapContainer.value,
  layers: [baseLayer],
  view: new View({
    center: [116.4, 39.9],
    zoom: 10,
    projection: 'EPSG:4326'
  })
})
```

## Change

在 map 赋值后添加:

```typescript
map.value = new Map({
  target: mapContainer.value,
  layers: [baseLayer],
  view: new View({
    center: [116.4, 39.9],
    zoom: 10,
    projection: 'EPSG:4326'
  })
})

// 暴露地图实例到全局，供调试使用
window.__map__ = map.value
```

## Usage

在浏览器控制台中:

```javascript
// 获取地图实例
window.__map__

// 获取视图
window.__map__.getView()

// 获取图层
window.__map__.getLayers()

// 缩放到指定位置
window.__map__.getView().setCenter([116.4, 39.9])
window.__map__.getView().setZoom(10)

// 触发渲染
window.__map__.render()
```

## Notes

- 这是一个开发调试用的临时功能，生产环境可以考虑移除或条件编译
- 使用 `__map__` 双下划线前缀表示内部使用
