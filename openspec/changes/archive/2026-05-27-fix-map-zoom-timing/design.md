# Design: fix-map-zoom-timing

## Technical Design

### Current Code

```typescript
source.addFeatures(features)

if (map.value && source.getExtent()) {
  map.value.getView().fit(source.getExtent(), {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

### Fixed Code

```typescript
source.addFeatures(features)

// 延迟调用 fit，确保 features 加载完成
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

### Key Changes

1. 使用 setTimeout 延迟 100ms
2. 添加 extent 有效性检查（防止 Infinity 或 NaN）
