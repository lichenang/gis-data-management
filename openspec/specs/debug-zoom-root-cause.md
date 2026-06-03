# 地图自动缩放失败根因诊断报告

## 问题描述

用户勾选地图图层后，GeoJSON 正常加载，但 `setTimeout` 内的 `fit` 调用未生效。

---

## 代码分析

### 当前实现 (MapContainer.vue:130-186)

```typescript
function loadLayer(layerInfo: LayerInfo) {
  const source: any = new VectorSource({
    loader: async (extent, resolution, projection) => {
      // ... fetch GeoJSON ...
      const features = new GeoJSON().readFeatures(text, {
        featureProjection: projection  // ← 问题点1
      })
      source.addFeatures(features)

      if (map.value) {
        setTimeout(() => {
          const extent = source.getExtent()
          if (extent && !isNaN(extent[0]) && !isNaN(extent[1])) {  // ← 问题点2
            map.value.getView().fit(extent, {...})
          }
        }, 100)
      }
    }
  })
}
```

---

## 根因分析

### 问题 1: extent 边界检查不完整 ❌ **关键问题**

**位置**: `MapContainer.vue:160`

```typescript
if (extent && !isNaN(extent[0]) && !isNaN(extent[1]))
```

**问题**:
- extent 格式为 `[minX, minY, maxX, maxY]`（4个值）
- 当前只检查了 `extent[0]` (minX) 和 `extent[1]` (minY)
- **未检查 `extent[2]` (maxX) 和 `extent[3]` (maxY)**

**场景**: 如果 API 返回的 GeoJSON 只有一个点要素（或空数据），extent 可能是:
```
[116.4, 39.9, NaN, NaN]  // 只有中心点，无范围
[116.4, 39.9, Infinity, Infinity]  // 异常数据
```

此时 `minX` 和 `minY` 是有效值，检查通过，但 `fit` 会失败或行为异常。

---

### 问题 2: 未处理空数据场景 ❌

**位置**: `MapContainer.vue:155`

**问题**: 如果 API 返回空 FeatureCollection:
```json
{"type":"FeatureCollection","features":[]}
```

- `source.addFeatures(features)` 成功执行（空数组）
- `source.getExtent()` 返回 `[Infinity, Infinity, -Infinity, -Infinity]`（OpenLayers 约定）
- 当前检查 `!isNaN(extent[0])` → `!isNaN(Infinity)` → `true`
- `fit(Infinity, ...)` 可能导致异常行为

---

### 问题 3: projection 参数可能未定义 ⚠️

**位置**: `MapContainer.vue:152-154`

```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: projection  // ← projection 可能为 undefined
})
```

**分析**:
- 在 OpenLayers VectorSource 中，如果未显式设置 `projection`，loader 的 projection 参数来自 `source.getProjection()`
- 默认情况下 VectorSource 使用 view 的投影 → 应该是 'EPSG:4326'
- **但在组件刚创建、map 尚未初始化时调用 loader，可能导致 projection 为 undefined**

**时序问题**:
```
watch(() => props.layers) 触发
    ↓
loadLayer() 被调用
    ↓
VectorSource 创建，loader 注册
    ↓
loader 首次被调用 ← 此时 map.value 已存在
```

实际上 map.value 在 loadLayer 开始时就检查了 (`if (!map.value) return`)，所以这不是主要问题。

---

### 问题 4: fit 被后续操作覆盖 🔄

**位置**: 多图层场景

**场景**:
- 用户勾选了 3 个图层
- 每个图层加载完成后都调用 `fit()`
- 最后一个完成的 fit 会覆盖前面的

**当前代码没有处理**:
- 只对最后一个图层执行 fit
- 或合并所有图层的 extent 再 fit

---

### 问题 5: CSS/容器尺寸问题 📐

**位置**: `MapContainer.vue:232-235`, `index.vue:60-73`

```css
.map-container {
  width: 100%;
  height: 100%;
}
```

```css
.map-viewer {
  width: 100%;
  height: 100%;
}
```

**检查项**:
- `el-main` 需要显式设置高度
- 父级容器链都需要有明确高度
- 如果容器尺寸为 0x0，`fit` 会静默失败

---

### 问题 6: setTimeout 时机问题 ⏱️

**位置**: `MapContainer.vue:158`

```typescript
setTimeout(() => {...}, 100)
```

**分析**:
- 100ms 在大多数场景下足够
- 但如果 map 还未完成渲染，可能导致问题
- 更好的方案: 使用 `map.once('rendercomplete', ...)` 或 `requestAnimationFrame`

---

## 修复方案

### 修复 1: 完整的 extent 边界检查 ✅

```typescript
function loadLayer(layerInfo: LayerInfo) {
  const source = new VectorSource({
    loader: async (extent, resolution, projection) => {
      // ... fetch ...
      const features = new GeoJSON().readFeatures(text, {
        featureProjection: projection || 'EPSG:4326'  // 防御性编程
      })
      source.addFeatures(features)

      if (map.value) {
        setTimeout(() => {
          const extent = source.getExtent()
          // 完整检查：所有4个坐标都必须是有穷数
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
            console.warn('Invalid extent:', extent)
          }
        }, 100)
      }
    },
    format: new GeoJSON()
  })

  // ...
}
```

---

### 修复 2: 合并多图层 extent ✅

```typescript
// 在 loadLayer 之外维护一个计数器
const loadingCount = ref(0)
const loadedLayers = ref<Set<number>>(new Set())

function loadLayer(layerInfo: LayerInfo) {
  loadingCount.value++
  
  const source = new VectorSource({
    loader: async (extent, resolution, projection) => {
      // ... fetch ...
      source.addFeatures(features)
      loadingCount.value--
      loadedLayers.value.add(layerInfo.id)
      
      // 所有图层都加载完成后再 fit
      if (loadingCount.value === 0 && map.value) {
        adjustViewToAllLayers()
      }
    },
    format: new GeoJSON()
  })
}

function adjustViewToAllLayers() {
  let combinedExtent: import('ol extent').Extent | null = null
  
  for (const layer of Object.values(vectorLayers.value)) {
    const source = layer.getSource()
    if (source) {
      const ext = source.getExtent()
      if (ext && isFinite(ext[0])) {
        if (!combinedExtent) {
          combinedExtent = [...ext]
        } else {
          import('ol extent').extend(combinedExtent, ext)
        }
      }
    }
  }
  
  if (combinedExtent && map.value) {
    map.value.getView().fit(combinedExtent, {
      padding: [50, 50, 50, 50],
      maxZoom: 15,
      duration: 500
    })
  }
}
```

---

### 修复 3: 使用 rendercomplete 事件 ✅

```typescript
function initMap() {
  map.value = new Map({
    target: mapContainer.value,
    layers: [baseLayer],
    view: new View({
      center: [116.4, 39.9],
      zoom: 10,
      projection: 'EPSG:4326'
    })
  })

  // 使用 rendercomplete 替代 setTimeout
  map.value.on('rendercomplete', () => {
    // 处理渲染完成后的操作
  })
}
```

---

## 诊断步骤

建议按以下步骤定位问题：

1. **添加日志**
```typescript
setTimeout(() => {
  const extent = source.getExtent()
  console.log('[Zoom] extent:', extent)
  console.log('[Zoom] map.value:', !!map.value)
  console.log('[Zoom] view:', map.value?.getView())
  
  if (extent && !isNaN(extent[0]) && !isNaN(extent[1])) {
    console.log('[Zoom] Calling fit...')
    map.value.getView().fit(extent, {...})
    console.log('[Zoom] fit called')
  }
}, 100)
```

2. **检查 API 返回**
- 使用浏览器 DevTools Network 面板
- 查看 `/api/v1/datasets/{id}/geojson` 返回的 GeoJSON
- 确认 `features` 数组不为空
- 确认坐标是有效的经纬度

3. **检查容器尺寸**
- 在浏览器控制台执行:
```javascript
document.querySelector('.map-container').getBoundingClientRect()
```
- 确保 width > 0 && height > 0

---

## 结论

**最可能的根因**: extent 检查不完整，导致 `Infinity` 或 `NaN` 值传递给 `fit()`

**修复优先级**:
1. 🔴 高: 修复 extent 边界检查（问题1）
2. 🟡 中: 处理空数据场景（问题2）
3. 🟢 低: 优化多图层 fit 逻辑（问题4）
