# Design: fix-map-extent-validation

## Overview

修复 `MapContainer.vue` 中 extent 验证逻辑的缺陷，确保地图在加载 GeoJSON 数据后正确自动缩放。

## Current Code

```typescript
// MapContainer.vue:157-168
if (map.value) {
  setTimeout(() => {
    const extent = source.getExtent()
    if (extent && !isNaN(extent[0]) && !isNaN(extent[1])) {
      map.value.getView().fit(extent, {
        padding: [50, 50, 50, 50],
        maxZoom: 15,
        duration: 500
      })
    }
  }, 100)
}
```

## Design

### Change 1: 完整的 extent 验证

替换原有的不完整检查:

```typescript
const extent = source.getExtent()
const isValidExtent = extent && 
  isFinite(extent[0]) && isFinite(extent[1]) &&
  isFinite(extent[2]) && isFinite(extent[3])

if (isValidExtent) {
  map.value.getView().fit(extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
} else {
  console.warn('[MapContainer] Invalid extent, skipping fit:', extent)
}
```

**理由**:
- `isFinite()` 比 `!isNaN()` 更严格，直接排除 Infinity 和 NaN
- 添加警告日志便于调试

### Change 2: projection 防御性编程

```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: projection || 'EPSG:4326'
})
```

**理由**: 避免 projection 为 undefined 时导致坐标转换失败。

### Change 3: 合并多图层 extent (可选增强)

当前每个图层加载完成都会调用 fit，多图层时会相互覆盖。改进方案:

```typescript
// 添加全局状态
const pendingFits = ref(0)

function loadLayer(layerInfo: LayerInfo) {
  pendingFits.value++
  
  const source = new VectorSource({
    loader: async (extent, resolution, projection) => {
      // ... fetch ...
      source.addFeatures(features)
      pendingFits.value--
      
      if (pendingFits.value === 0) {
        fitAllLayers()
      }
    }
  })
}

function fitAllLayers() {
  let combinedExtent: import('ol/extent').Extent | null = null
  
  for (const layer of Object.values(vectorLayers.value)) {
    const source = layer.getSource()
    const ext = source?.getExtent()
    if (ext && isFinite(ext[0])) {
      combinedExtent = combinedExtent 
        ? import('ol/extent').extend(combinedExtent, ext)
        : [...ext]
    }
  }
  
  if (combinedExtent) {
    map.value.getView().fit(combinedExtent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
```

## Implementation Notes

- 使用 `isFinite()` 而非 `!isNaN()`: 更准确排除 Infinity
- 保留 100ms setTimeout: 确保 map 渲染完成
- 添加 console.warn: 不阻塞用户，但便于问题排查
