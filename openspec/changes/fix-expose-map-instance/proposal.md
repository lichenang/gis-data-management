# Proposal: fix-expose-map-instance

## Summary

在 MapContainer.vue 中暴露地图实例到 `window.__map__`，便于在浏览器控制台调试。

## Problem

用户在浏览器控制台调试地图相关问题时，无法直接访问 OpenLayers 的 Map 实例。当前没有简便的方式查看或操作地图状态。

## Solution

在 `initMap()` 函数中，map 创建并赋值后，添加一行:

```typescript
window.__map__ = map.value
```

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Function: `initMap` (around line 91)

## Success Criteria

- [ ] 在浏览器控制台可以通过 `window.__map__` 访问 Map 实例
- [ ] `window.__map__.getView()` 返回 View 对象
- [ ] `window.__map__.getLayers()` 返回图层数组
