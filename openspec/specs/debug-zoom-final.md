# 地图 fit() 失败最终诊断报告

## 问题描述

用户勾选图层后，GeoJSON 加载成功（Network 返回 200），但 `map.getView().fit(source.getExtent(), ...)` 没有任何效果，地图视图未发生变化。

---

## 逐行分析

### Q1: map 对象是在哪里创建的？是否正确赋值给 map.value？

**答案**: ✅ 正确

| 行号 | 代码 | 说明 |
|------|------|------|
| 76-91 | `initMap()` 函数 | 创建 Map 实例 |
| 83 | `map.value = new Map({...})` | 正确赋值 |
| 86-90 | View 配置 | projection: 'EPSG:4326' ✓ |

```typescript
// Line 83-91
map.value = new Map({
  target: mapContainer.value,
  layers: [baseLayer],
  view: new View({
    center: [116.4, 39.9],
    zoom: 10,
    projection: 'EPSG:4326'
  })
})
```

---

### Q2 & Q3: source 是否添加到 VectorLayer？VectorLayer 是否添加到 map？

**答案**: ✅ 正确

| 行号 | 代码 | 说明 |
|------|------|------|
| 134 | `const source = new VectorSource({...})` | 创建 VectorSource |
| 179-188 | `new VectorLayer({ source, ... })` | 创建包含 source 的 VectorLayer |
| 190 | `map.value.addLayer(layer)` | 添加到地图 |

```typescript
// Line 134-177: 创建 source
const source: any = new VectorSource({
  loader: async (extent, resolution, projection) => {
    // ... fetch GeoJSON ...
    source.addFeatures(features)  // Line 155
    // ... fit logic ...
  }
})

// Line 179-191: 创建 layer 并添加
const layer = new VectorLayer({
  source,  // ← 同一个 source 实例
  style: {...}
})
map.value.addLayer(layer)  // ← 已添加到地图
vectorLayers.value[layerInfo.id] = layer
```

---

### Q4: fit 调用时 source.getExtent() 的实际返回值？

**当前实现 (Line 159-172)**:

```typescript
setTimeout(() => {
  const extent = source.getExtent()  // Line 159
  const isValidExtent = extent &&
    isFinite(extent[0]) && isFinite(extent[1]) &&
    isFinite(extent[2]) && isFinite(extent[3])

  if (isValidExtent) {
    map.value.getView().fit(extent, {...})  // Line 165
  } else {
    console.warn('[MapContainer] Invalid extent, skipping fit:', extent)
  }
}, 100)
```

**问题**: 这个修复是必要的，但可能不是根本原因。

---

### Q5: fit 的参数 padding 和 duration 是否导致问题？

**答案**: ⚠️ 不是主要问题，但有隐患

| 参数 | 当前值 | 评估 |
|------|--------|------|
| padding | `[50, 50, 50, 50]` | 正常值 |
| duration | `500` (毫秒) | 正常值 |

这些参数不会导致 fit 完全失效。

---

### Q6: map 的 projection 与数据坐标系是否一致？

**答案**: ✅ View 是一致的，但 ❌ **Source 缺少 projection**

| 位置 | 代码 | projection |
|------|------|------------|
| Line 89 | `view: new View({ projection: 'EPSG:4326' })` | EPSG:4326 ✓ |
| Line 134 | `new VectorSource({ loader: ... })` | **未设置** ❌ |

**关键问题**: VectorSource 没有设置 `projection` 属性！

当 VectorSource 没有显式设置 projection 时:
- `source.getProjection()` 返回 `null` 或 undefined
- loader 回调中的 `projection` 参数可能不正确或 undefined
- `source.getExtent()` 可能在错误的坐标系下计算

---

### Q7: 容器尺寸是否可能为 0？

**答案**: ✅ 不是问题

```
#app (App.vue:15-19)
  └─ .map-viewer (index.vue:60-63: height: 100%)
       └─ el-container
            └─ el-main (padding: 0)
                 └─ .map-container (MapContainer.vue:238-241: width/height: 100%)
```

链路完整，App.vue 显式设置了 `#app { height: 100vh }`，容器尺寸不会是 0。

---

### Q8: 是否有其他代码在 fit 后重置视图？

**答案**: ✅ 未发现

搜索整个 MapContainer.vue，没有发现其他 `setCenter`、`setZoom` 调用。

---

## 🔴 根本原因定位

### 核心问题: VectorSource 缺少 projection 配置

**位置**: `MapContainer.vue:134`

```typescript
// 当前代码 (错误)
const source = new VectorSource({
  loader: async (extent, resolution, projection) => {
    // ...
  }
})
```

**问题解释**:

```
┌─────────────────────────────────────────────────────────────────┐
│                    OpenLayers 数据加载流程                       │
└─────────────────────────────────────────────────────────────────┘

VectorSource 创建
    ↓
无 explicit projection → source.getProjection() = null
    ↓
loader 被调用 (使用 'all' 策略，触发立即加载)
    ↓
loader 内部: new GeoJSON().readFeatures(text, {
  featureProjection: projection || 'EPSG:4326'  ← projection 可能是 null
})
    ↓
source.addFeatures(features)  ← Features 被添加到无 projection 的 source
    ↓
source.getExtent()  ← 在错误的上下文计算extent？
    ↓
map.getView().fit(extent)  ← extent 坐标系可能与 view 不匹配
```

### 为什么 fit 看起来"没有反应"？

最可能的情况:
1. **extent 计算正确，但 fit 参数无效** - extent 可能是 `[NaN, NaN, NaN, NaN]`
2. **extent 坐标系与 view 不匹配** - source 不知道自己的 projection，导致 extent 是基于错误的坐标系

---

## 修复方案

### 方案 A: 为 VectorSource 显式设置 projection ✅ 推荐

```typescript
// MapContainer.vue:134-177
const source = new VectorSource({
  projection: 'EPSG:4326',  // ← 添加这一行
  loader: async (extent, resolution, projection) => {
    const token = localStorage.getItem('access_token')
    const url = `/api/v1/datasets/${layerInfo.id}/geojson`

    const response = await fetch(url, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })

    if (!response.ok) {
      console.error('Failed to load GeoJSON:', response.status)
      return
    }

    const text = await response.text()
    const features = new GeoJSON().readFeatures(text, {
      featureProjection: 'EPSG:4326'  // 明确指定
    })
    source.addFeatures(features)

    if (map.value) {
      setTimeout(() => {
        const extent = source.getExtent()
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
          console.warn('[MapContainer] Invalid extent:', extent)
        }
      }, 100)
    }
  },
  format: new GeoJSON()
})
```

### 方案 B: 为 extent 添加额外验证

在调用 fit 前验证 extent 是否在合理范围内:

```typescript
setTimeout(() => {
  const extent = source.getExtent()
  
  // 额外检查: extent 范围是否合理 (经度 -180~180, 纬度 -90~90)
  const isInValidBounds = 
    extent[0] >= -180 && extent[0] <= 180 &&
    extent[1] >= -90 && extent[1] <= 90 &&
    extent[2] >= -180 && extent[2] <= 180 &&
    extent[3] >= -90 && extent[3] <= 90

  if (isValidExtent && isInValidBounds) {
    map.value.getView().fit(extent, {...})
  } else {
    console.warn('[MapContainer] Extent out of bounds:', extent)
  }
}, 100)
```

---

## 最终修复代码

修改 `MapContainer.vue:134`，为 VectorSource 添加 projection:

```diff
- const source: any = new VectorSource({
+ const source: any = new VectorSource({
+   projection: 'EPSG:4326',
    loader: async (extent: any, resolution: any, projection: any) => {
```

同时修改 Line 153，明确指定 featureProjection:

```diff
  const features = new GeoJSON().readFeatures(text, {
-   featureProjection: projection || 'EPSG:4326'
+   featureProjection: 'EPSG:4326'
  })
```

---

## 诊断验证清单

在浏览器控制台执行以下命令验证:

```javascript
// 1. 检查 map 实例
console.log('map:', !!map.value)

// 2. 检查 layer 和 source
const layers = map.value.getLayers().getArray()
const vectorLayer = layers.find(l => l.get('layerId'))
const source = vectorLayer.getSource()

// 3. 检查 source projection
console.log('source projection:', source.getProjection())

// 4. 检查 extent
console.log('extent:', source.getExtent())

// 5. 检查 view projection
console.log('view projection:', map.value.getView().getProjection().getCode())
```

如果 `source.getProjection()` 返回 `null`，则确认了问题。
