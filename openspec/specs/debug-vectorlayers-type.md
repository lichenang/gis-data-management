# vectorLayers.has 报错问题 - 诊断报告

## 问题描述

Console 报错：
```
MapContainer.vue:168 - vectorLayers.value.has is not a function
```

## 根因分析

### 代码检查

**初始化** (第 48 行):
```typescript
const vectorLayers = ref<Map<number, VectorLayerType>>(new Map())
```

**使用** (第 168 行):
```typescript
if (!vectorLayers.value.has(layer.id)) {
```

### 问题分析

Vue 3 的 `ref()` 对复杂对象（Map、Set）的支持有限。虽然代码声明为 `ref<Map<number, VectorLayerType>>(new Map())`，但 Vue 的响应式系统不会保持 Map 的原生方法正常工作。

当调用 `vectorLayers.value.has()` 时，`vectorLayers.value` 可能不是真正的 Map 对象，导致 `.has()` 方法不存在。

```
ref(new Map())
    │
    ▼
Vue 响应式处理
    │
    ▼
vectorLayers.value 可能是 { value: Map } 或被转换的对象
    │
    ▼
.has() 方法不可用 ❌
```

## 修复方案

### 方案 1: 改为普通对象（推荐）

将 Map 改为普通对象：

```typescript
// 修改前 (第 48 行)
const vectorLayers = ref<Map<number, VectorLayerType>>(new Map())

// 修改后
const vectorLayers = ref<Record<number, VectorLayerType>>({})

// 同时修改相关代码：
// .has(id) → id in vectorLayers.value
// .set(id, layer) → vectorLayers.value[id] = layer
// .get(id) → vectorLayers.value[id]
// .delete(id) → delete vectorLayers.value[id]
```

### 方案 2: 改为数组

```typescript
// 修改前
const vectorLayers = ref<Map<number, VectorLayerType>>(new Map())

// 修改后
const vectorLayers = ref<{ id: number; layer: VectorLayerType }[]>([])

// 同时修改相关代码
// .has(id) → vectorLayers.value.some(l => l.id === id)
// .set(id, layer) → vectorLayers.value.push({ id, layer })
// .get(id) → vectorLayers.value.find(l => l.id === id)
// .delete(id) → vectorLayers.value = vectorLayers.value.filter(l => l.id !== id)
```

## 修改清单

| 行号 | 修改前 | 修改后 |
|------|--------|--------|
| 48 | `ref<Map<number, VectorLayerType>>(new Map())` | `ref<Record<number, VectorLayerType>>({})` |
| 150 | `vectorLayers.value.set(layerInfo.id, layer)` | `vectorLayers.value[layerInfo.id] = layer` |
| 156 | `vectorLayers.value.get(layerId)` | `vectorLayers.value[layerId]` |
| 159 | `vectorLayers.value.delete(layerId)` | `delete vectorLayers.value[layerId]` |
| 168 | `vectorLayers.value.has(layer.id)` | `layer.id in vectorLayers.value` |
