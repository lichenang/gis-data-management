# Tasks: fix-map-zoom-timing

## Task 1: 修改 fit 调用使用 setTimeout

**File**: `frontend/src/views/map/MapContainer.vue`

**修改 loadLayer 函数的 loader 回调**:

将：
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

改为：
```typescript
source.addFeatures(features)

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

## Task 2: 验证

1. 访问地图页面
2. 勾选已发布数据集
3. 验证地图自动缩放到数据范围
