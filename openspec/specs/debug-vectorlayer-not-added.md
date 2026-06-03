# 矢量图层未添加到地图 - 诊断报告

## 问题描述

用户反映地图页面只显示底图（1个图层），矢量图层未被添加到地图。
浏览器诊断显示 `map.getAllLayers()` 仅返回 1 个图层。

---

## 数据流分析

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         数据流拓扑图                                      │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     emit('layer-change')     ┌──────────────────┐
│  LayerPanel  │ ─────────────────────────────▶│     index.vue    │
│              │    [selectedLayers array]     │                  │
└──────────────┘                               │ selectedLayers   │
                                               │        │         │
       │                                       │        ▼         │
       ▼                                       │  <MapContainer   │
  User checks                                   │   :layers=       │
  checkbox                                      └────────┬─────────┘
                                                          │
                                                          │ props.layers
                                                          ▼
                                               ┌──────────────────┐
                                               │  MapContainer    │
                                               │                  │
                                               │ watch (line 207) │
                                               │        │         │
                                               │        ▼         │
                                               │ loadLayer()      │
                                               │        │         │
                                               │        ▼         │
                                               │ map.addLayer()   │
                                               └──────────────────┘
```

---

## 逐项分析

### Q1: 矢量图层是否在某个条件下才被创建和添加？这个条件是否满足？

**查看代码**:
- `loadLayer()` Line 132-195
- Line 133: `if (!map.value) return`

**分析**:
```typescript
function loadLayer(layerInfo: LayerInfo) {
  if (!map.value) return  // ← 关键：map 必须已初始化
  // ... 创建 source, layer, addLayer ...
}
```

**问题**: `loadLayer` 依赖 `map.value !== null`

**初始化顺序**:
```
组件创建
    ↓
watch 立即执行 (props.layers = [])
    ↓
loadLayer([]) 被调用 → map.value 为 null → 直接 return (无效!)
    ↓
onMounted() → initMap() → map.value 被赋值
```

**但是**: 当用户勾选复选框时，`props.layers` 变为非空数组，此时 map.value 应该已经存在。

---

### Q2: loader 函数是否被正确绑定到 VectorSource？

**查看代码** Line 136-180:

```typescript
const source: any = new VectorSource({
  projection: 'EPSG:4326',
  loader: async (extent, resolution, projection) => {
    // ... fetch GeoJSON ...
    source.addFeatures(features)
    // ... fit logic ...
  },
  format: new GeoJSON()
})
```

**分析**: ✅ Loader 正确绑定

VectorSource 的 loader 是一个 async 函数，内部通过闭包引用 `source` 变量，可以正确调用 `source.addFeatures()`。

---

### Q3: 矢量图层是否被 map.addLayer() 调用？

**查看代码** Line 192-194:

```typescript
const layer = new VectorLayer({
  source,
  style: {...},
  properties: { layerId: layerInfo.id }
})

map.value.addLayer(layer)  // ← 确实被调用
vectorLayers.value[layerInfo.id] = layer
```

**分析**: ✅ `map.addLayer()` 被调用

---

### Q4: watch 监听 layers prop 的逻辑是否正确？

**查看代码** Line 207-222:

```typescript
watch(() => props.layers, (newLayers, oldLayers) => {
  const oldIds = new Set((oldLayers || []).map(l => l.id))
  const newIds = new Set(newLayers.map(l => l.id))

  newLayers.forEach(layer => {
    if (!(layer.id in vectorLayers.value)) {  // ← 条件判断
      loadLayer(layer)
    }
  })

  oldIds.forEach(id => {
    if (!newIds.has(id)) {
      unloadLayer(id)
    }
  })
}, { deep: true })
```

**分析**:
1. ✅ 使用 `{ deep: true }` 监听数组内容变化
2. ✅ 检查 `layer.id in vectorLayers.value` 避免重复加载
3. ✅ 正确遍历添加和移除

---

## 真实根因：时序问题

### 问题1: watch 首次执行时 map 尚未初始化

```typescript
// MapContainer.vue

// 1. 组件 props 传入 (layers = [])
watch(() => props.layers, (newLayers) => {
  newLayers.forEach(layer => {
    if (!(layer.id in vectorLayers.value)) {
      loadLayer(layer)  // ← 此时 map.value 可能是 null!
    }
  })
}, { deep: true })

// 2. onMounted 时才初始化 map
onMounted(() => {
  initMap()  // ← map.value 在这里才被赋值
})
```

**如果**用户首次访问页面就勾选图层（在 onMounted 之前），图层会被静默跳过。

### 问题2: API 返回空数据导致无图层可选

查看 LayerPanel.vue Line 92-111:

```typescript
async function fetchPublishedLayers() {
  const response = await getPublishedDatasets()
  if (response.code === 200) {
    layers.value = (response.data || []).map(...)
  }
}
```

如果后端返回空数组或 API 失败，`layers.value = []`，用户无法选择任何图层。

---

## 调试验证步骤

在浏览器控制台执行:

```javascript
// 1. 检查 map 实例
console.log('Map:', !!window.__map__)

// 2. 检查所有图层
const layers = window.__map__.getAllLayers()
console.log('Total layers:', layers.length)
layers.forEach((l, i) => console.log(`Layer ${i}:`, l.get('layerId'), l instanceof ol.layer.Vector))

// 3. 检查 props.layers 是否传递正确
// 需要在 Vue Devtools 中查看 MapContainer 组件的 props.layers

// 4. 检查 vectorLayers 存储
// 需要在 MapContainer 实例中查看 vectorLayers.value
```

---

## 修复方案

### 方案1: 延迟图层加载，确保 map 已初始化

```typescript
// 修改 loadLayer 函数
function loadLayer(layerInfo: LayerInfo) {
  // 如果 map 尚未初始化，使用 nextTick
  if (!map.value) {
    import { nextTick } from 'vue'
    nextTick(() => {
      if (map.value) {
        doLoadLayer(layerInfo)
      }
    })
    return
  }
  doLoadLayer(layerInfo)
}

function doLoadLayer(layerInfo: LayerInfo) {
  // 原有加载逻辑
}
```

### 方案2: 添加主动刷新逻辑

```typescript
// 当 map 初始化完成后，检查是否需要加载图层
onMounted(() => {
  initMap()
  
  // map 初始化完成后，如果已有待加载图层，立即加载
  if (props.layers.length > 0) {
    props.layers.forEach(layer => {
      if (!(layer.id in vectorLayers.value)) {
        loadLayer(layer)
      }
    })
  }
})
```

### 方案3: 调试日志

```typescript
// 在 loadLayer 开头添加
function loadLayer(layerInfo: LayerInfo) {
  console.log('[MapContainer] loadLayer called:', layerInfo.id, 'map.value:', !!map.value)
  
  if (!map.value) {
    console.warn('[MapContainer] Map not ready, skipping loadLayer')
    return
  }
  // ...
}
```

---

## 结论

**最大可能根因**: `loadLayer()` 在 `map.value` 为 `null` 时被调用，导致矢量图层静默添加失败。

**验证方法**: 在 `loadLayer` 开头添加 console.log，观察调用时 map.value 的状态。

**修复优先级**:
1. 🔴 高: 修改 loadLayer 使用 nextTick 或在 onMounted 后主动加载
2. 🟡 中: 添加调试日志确认问题
3. 🟢 低: 检查后端 API 返回是否正常
