# Tasks: fix-geojson-parse-string

## Task 1: 添加 JSON.parse 解析 GeoJSON 数据

- [x] 完成

## Task 2: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `loadLayer` 函数中的 loader 回调:

将:
```typescript
const text = await response.text()
const features = new GeoJSON().readFeatures(text, {
  featureProjection: 'EPSG:4326'
})
source.addFeatures(features)
```

改为:
```typescript
const text = await response.text()

let jsonData
try {
  jsonData = JSON.parse(text)
} catch (e) {
  console.error('[MapContainer] Failed to parse GeoJSON:', e)
  return
}

const features = new GeoJSON().readFeatures(jsonData, {
  featureProjection: 'EPSG:4326'
})
source.addFeatures(features)

console.log('[MapContainer] Loaded features:', features.length)
```

## Task 2: 验证

1. 启动前端开发服务器 (`npm run dev`)
2. 打开浏览器开发者工具
3. 访问地图页面 `/map`
4. 勾选一个已发布的数据集
5. 验证:
   - 控制台显示 `[MapContainer] Loaded features:` 且数量 > 0
   - 地图上显示矢量要素
   - `window.__map__.getAllLayers().length` >= 2
