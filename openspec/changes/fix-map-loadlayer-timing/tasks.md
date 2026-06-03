# Tasks: fix-map-loadlayer-timing

## Task 1: 在 onMounted 中添加图层加载逻辑

- [x] 完成

## Task 2: 在 loadLayer 中添加调试日志

- [x] 完成

## Task 3: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `onMounted` 函数:

将:
```typescript
onMounted(() => {
  initMap()
})
```

改为:
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

## Task 2: 在 loadLayer 中添加调试日志

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `loadLayer` 函数开头:

将:
```typescript
function loadLayer(layerInfo: LayerInfo) {
  if (!map.value) return
```

改为:
```typescript
function loadLayer(layerInfo: LayerInfo) {
  console.log('[MapContainer] loadLayer called:', layerInfo.id, 'map:', !!map.value)
  
  if (!map.value) {
    console.warn('[MapContainer] Map not ready, skipping layer:', layerInfo.id)
    return
  }
```

## Task 3: 验证

1. 启动前端开发服务器 (`npm run dev`)
2. 打开浏览器开发者工具
3. 访问地图页面 `/map`
4. 在控制台观察日志输出
5. 勾选一个已发布的数据集
6. 验证:
   - 控制台显示 `[MapContainer] loadLayer called:`
   - `window.__map__.getAllLayers().length` 应为 2 (底图 + 矢量)
   - 矢量图层在地图上可见
