# Tasks: fix-map-extent-validation

## Task 1: 修复 extent 边界检查

- [x] 完成

## Task 2: 添加 projection 防御

- [x] 完成

## Task 3: 验证

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `loadLayer` 函数中的 loader 回调:

将:
```typescript
if (extent && !isNaN(extent[0]) && !isNaN(extent[1])) {
  map.value.getView().fit(extent, {
    padding: [50, 50, 50, 50],
    maxZoom: 15,
    duration: 500
  })
}
```

改为:
```typescript
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

## Task 2: 添加 projection 防御

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** GeoJSON 解析配置:

将:
```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: projection
})
```

改为:
```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: projection || 'EPSG:4326'
})
```

## Task 3: 验证

- [x] 完成

1. 启动前端开发服务器
2. 访问地图页面 (`/map`)
3. 勾选一个已发布的数据集
4. 验证地图自动缩放到数据范围
5. 勾选多个数据集，验证缩放行为正确
6. 检查浏览器控制台无错误输出
