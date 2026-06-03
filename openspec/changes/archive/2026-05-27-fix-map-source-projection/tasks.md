# Tasks: fix-map-source-projection

## Task 1: 添加 VectorSource projection 配置

- [x] 完成

## Task 2: 明确指定 featureProjection

- [x] 完成

## Task 3: 验证

- [x] 完成 (需手动测试)

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** `loadLayer` 函数中的 VectorSource 构造函数:

将:
```typescript
const source: any = new VectorSource({
  loader: async (extent: any, resolution: any, projection: any) => {
```

改为:
```typescript
const source: any = new VectorSource({
  projection: 'EPSG:4326',
  loader: async (extent: any, resolution: any, projection: any) => {
```

## Task 2: 明确指定 featureProjection

**File**: `frontend/src/views/map/MapContainer.vue`

**修改** GeoJSON 解析配置:

将:
```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: projection || 'EPSG:4326'
})
```

改为:
```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: 'EPSG:4326'
})
```

## Task 3: 验证

1. 启动前端开发服务器 (`npm run dev`)
2. 打开浏览器开发者工具
3. 访问地图页面 `/map`
4. 勾选一个已发布的数据集
5. 在控制台执行验证脚本:

```javascript
const layers = map.value.getLayers().getArray()
const vectorLayer = layers.find(l => l.get('layerId'))
const source = vectorLayer.getSource()

console.log('Source projection:', source.getProjection()?.getCode())
console.log('Extent:', source.getExtent())
```

6. 验证:
   - `source.getProjection()` 应返回非 null
   - `source.getExtent()` 应返回有效数值
   - 地图应自动缩放到数据范围
