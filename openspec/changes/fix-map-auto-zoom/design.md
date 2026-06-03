# Design: fix-map-auto-zoom

## Technical Design

### Modification

在 loadLayer 函数的 loader 完成加载后，添加 fit 视图调用：

```typescript
const source: any = new VectorSource({
  loader: async (extent, resolution, projection) => {
    // ... existing loader code ...
    
    // 加载完成后自动缩放到数据范围
    if (map.value && source.getExtent()) {
      map.value.getView().fit(source.getExtent(), {
        padding: [50, 50, 50, 50],
        maxZoom: 15,
        duration: 500
      })
    }
  }
})
```

### Parameters

- `padding`: 视图边缘留白 [top, right, bottom, left]
- `maxZoom`: 最大缩放级别，防止过度放大
- `duration`: 动画过渡时间（毫秒）
