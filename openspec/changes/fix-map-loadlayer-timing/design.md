# Design: fix-map-loadlayer-timing

## Overview

修复 MapContainer 组件初始化时序问题，确保矢量图层正确添加到地图。

## Current Code

### 问题代码 1: onMounted (Line 229-231)

```typescript
onMounted(() => {
  initMap()
})
```

### 问题代码 2: loadLayer (Line 132-133)

```typescript
function loadLayer(layerInfo: LayerInfo) {
  if (!map.value) return  // ← 静默失败，无日志
  // ...
}
```

## Changes

### Change 1: onMounted 中重新加载图层

```typescript
onMounted(() => {
  initMap()
  
  // map 初始化完成后，加载已选中的图层
  if (props.layers.length > 0) {
    props.layers.forEach(layer => {
      if (!(layer.id in vectorLayers.value)) {
        loadLayer(layer)
      }
    })
  }
})
```

**理由**: 确保 map 完全初始化后再执行图层加载。

### Change 2: 添加 loadLayer 调试日志

```typescript
function loadLayer(layerInfo: LayerInfo) {
  console.log('[MapContainer] loadLayer called:', layerInfo.id, 'map:', !!map.value)
  
  if (!map.value) {
    console.warn('[MapContainer] Map not ready, skipping layer:', layerInfo.id)
    return
  }
  // ...
}
```

**理由**: 便于后续排查类似问题。

## Implementation Notes

- 使用 `props.layers.forEach` 遍历，调用 `loadLayer` 逐个加载
- 检查 `layer.id in vectorLayers.value` 避免重复加载同一图层
- 添加条件判断，只在有图层时执行加载
