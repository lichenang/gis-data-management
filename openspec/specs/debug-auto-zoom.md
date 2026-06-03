# 地图自动缩放未生效 - 诊断报告

## 问题描述

GeoJSON 请求成功返回 200 数据，但地图未自动缩放到数据范围。

## 可能原因

### 1. source.getExtent() 返回 undefined

OpenLayers 的 VectorSource 需要在 features 完全加载后才有正确的 extent。由于 loader 是异步的，`source.getExtent()` 可能在 features 添加前就被调用了。

### 2. 需要使用 loadend 事件

应该监听 source 的 `featuresloadend` 事件或使用 `once('addfeature')` 后再调用 fit。

## 修复方案

修改 loadLayer 函数，使用 `source.once('addfeature')` 或 `source.on('change')` 确保在 features 添加完成后调用 fit：

```typescript
loader: async (extent, resolution, projection) => {
  // ... existing loading code ...
  
  source.addFeatures(features)
  
  // 等待 features 添加完成后调用 fit
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
}
```

或者使用 OpenLayers 事件监听：

```typescript
source.on('addfeature', () => {
  if (map.value) {
    const extent = source.getExtent()
    if (extent) {
      map.value.getView().fit(extent, {
        padding: [50, 50, 50, 50],
        maxZoom: 15
      })
    }
  }
})
```

## 保存诊断结果

请将本报告保存为 `openspec/specs/debug-auto-zoom.md`，用户确认后创建修复变更。
