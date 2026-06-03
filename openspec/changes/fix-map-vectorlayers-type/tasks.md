# Tasks: fix-map-vectorlayers-type

## Task 1: 修复 vectorLayers 类型和所有相关调用

**File**: `frontend/src/views/map/MapContainer.vue`

**修改点**:

1. 第 48 行 - 初始化:
```typescript
// 修改前
const vectorLayers = ref<Map<number, VectorLayerType>>(new Map())

// 修改后
const vectorLayers = ref<Record<number, VectorLayerType>>({})
```

2. 第 150 行 - loadLayer 函数中:
```typescript
// 修改前
vectorLayers.value.set(layerInfo.id, layer)

// 修改后
vectorLayers.value[layerInfo.id] = layer
```

3. 第 156 行 - unloadLayer 函数中获取:
```typescript
// 修改前
const layer = vectorLayers.value.get(layerId)

// 修改后
const layer = vectorLayers.value[layerId]
```

4. 第 159 行 - unloadLayer 函数中删除:
```typescript
// 修改前
vectorLayers.value.delete(layerId)

// 修改后
delete vectorLayers.value[layerId]
```

5. 第 168 行 - watch 中检查:
```typescript
// 修改前
if (!vectorLayers.value.has(layer.id)) {

// 修改后
if (!(layer.id in vectorLayers.value)) {
```

## Task 2: 验证

1. 刷新地图页面
2. 验证无 Console 报错
3. 勾选图层验证显示正确
