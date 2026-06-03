# Tasks: fix-map-auto-zoom

## Task 1: 添加 auto-fit 逻辑

**File**: `frontend/src/views/map/MapContainer.vue`

**修改 loadLayer 函数的 loader 回调**:

在 `source.addFeatures(features)` 后添加：

```typescript
// 加载完成后自动缩放到数据范围
if (map.value && source.getExtent()) {
  map.value.getView().fit(source.getExtent(), {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

## Task 2: 验证

1. 重启前端
2. 访问地图页面
3. 勾选已发布数据集
4. 验证地图自动缩放到数据范围
