# Design: fix-map-vectorlayers-type

## Technical Design

### 修改清单

| 行号 | 修改前 | 修改后 |
|------|--------|--------|
| 48 | `ref<Map<number, VectorLayerType>>(new Map())` | `ref<Record<number, VectorLayerType>>({})` |
| 150 | `vectorLayers.value.set(layerInfo.id, layer)` | `vectorLayers.value[layerInfo.id] = layer` |
| 156 | `vectorLayers.value.get(layerId)` | `vectorLayers.value[layerId]` |
| 159 | `vectorLayers.value.delete(layerId)` | `delete vectorLayers.value[layerId]` |
| 168 | `vectorLayers.value.has(layer.id)` | `layer.id in vectorLayers.value` |

### 映射关系

```typescript
// Map → 普通对象
map.has(key)        →  key in obj
map.set(key, value) →  obj[key] = value
map.get(key)        →  obj[key]
map.delete(key)     →  delete obj[key]
```
