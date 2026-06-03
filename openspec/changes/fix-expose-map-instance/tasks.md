# Tasks: fix-expose-map-instance

## Task 1: 暴露地图实例到 window.__map__

- [x] 完成

## Task 2: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `initMap` 函数:

在 `map.value = new Map({...})` 块之后添加:

```typescript
// 暴露地图实例到全局，供调试使用
window.__map__ = map.value
```

## Task 2: 验证

1. 启动前端开发服务器 (`npm run dev`)
2. 打开浏览器开发者工具
3. 访问地图页面 `/map`
4. 在控制台执行验证:

```javascript
console.log('Map instance:', window.__map__)
console.log('View:', window.__map__.getView())
console.log('Layers:', window.__map__.getLayers())
```

5. 验证:
   - `window.__map__` 应返回 Map 实例
   - `.getView()` 应返回 View 对象
   - `.getLayers()` 应返回包含底图和矢量图层的数组
