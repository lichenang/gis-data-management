# Loader 函数要素数为 0 - 诊断报告

## 问题描述

用户反映即使添加了 JSON.parse，GeoJSON 数据的要素数仍为 0。

---

## Loader 函数完整代码分析

```typescript
// MapContainer.vue:144-194
loader: async (extent: any, resolution: any, projection: any) => {
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
  // ... fit logic
}
```

---

## 逐项分析

### Q1: JSON.parse 是否被正确调用？解析后的对象结构是否符合 GeoJSON FeatureCollection 格式？

**检查**: Line 159-167

```typescript
const text = await response.text()

let jsonData
try {
  jsonData = JSON.parse(text)
} catch (e) {
  console.error('[MapContainer] Failed to parse GeoJSON:', e)
  return
}
```

**分析**: ✅ JSON.parse 被正确调用，解析后的对象应该是有效的 JavaScript 对象。

**可能的 API 响应格式问题**:
- 如果 API 返回的是 `{ "data": "{\"type\":\"FeatureCollection\"...}" }` (双重嵌套)，需要两次解析
- 如果 API 返回的是 `{ "data": { "type": "FeatureCollection"... } }` (data 是对象)，不需要 JSON.parse
- 需检查控制台 `console.log('[MapContainer] Loaded features:', features.length)` 的输出

---

### Q2: readFeatures 是否接收了正确的参数？

**检查**: Line 169-171

```typescript
const features = new GeoJSON().readFeatures(jsonData, {
  featureProjection: 'EPSG:4326'
})
```

**分析**: ✅ readFeatures 接收了正确的参数:
- `jsonData`: 解析后的对象
- `featureProjection: 'EPSG:4326'`: 与 View 的 projection 一致

**潜在问题**:
- 如果 `jsonData` 是 **字符串** (而非对象)，readFeatures 会尝试解析它，但可能因为已经是一次 JSON 字符串而导致问题
- 需验证控制台打印的 `jsonData` 是什么样的结构

---

### Q3: 是否有 try-catch 将错误静默吞掉？

**检查**: Line 162-167, 154-157

```typescript
// JSON.parse 错误处理
try {
  jsonData = JSON.parse(text)
} catch (e) {
  console.error('[MapContainer] Failed to parse GeoJSON:', e)
  return  // ← 静默返回，不影响流程
}

// HTTP 错误处理
if (!response.ok) {
  console.error('Failed to load GeoJSON:', response.status)
  return  // ← 静默返回，不影响流程
}
```

**分析**: ⚠️ **是，存在静默返回**:
- 如果 JSON.parse 抛出异常，会打印错误并 return
- 如果 HTTP 响应不 ok，会打印错误并 return

**但**: 这不是导致 features 为 0 的原因，因为如果有错误，会 return 不会继续执行 readFeatures。

---

### Q4: addFeatures 是否被调用？如果调用了，传入的参数是否正确？

**检查**: Line 172

```typescript
source.addFeatures(features)
```

**分析**: ✅ `addFeatures(features)` 被调用，传入的是 `readFeatures()` 的返回值（Feature 数组）。

**注意事项**:
- `source` 是 VectorSource 实例 (Line 141)
- `features` 应该是 Feature[] 类型

---

### Q5: loader 函数是否在 loadLayer 中被正确绑定到 VectorSource？

**检查**: Line 141-196

```typescript
const source: any = new VectorSource({
  projection: 'EPSG:4326',
  loader: async (extent: any, resolution: any, projection: any) => {
    // ...
  },
  format: new GeoJSON()
} as any)
```

**分析**: ⚠️ **存在重大问题!**

## 🔴 根本原因: VectorSource loader 使用 async 函数

### 问题解释

OpenLayers 的 VectorSource loader 有特定的调用约定:

```
VectorSource 创建
    ↓
loader 被触发 (默认使用 'all' extent)
    ↓
OpenLayers 期望 loader 作为同步或回调方式工作
    ↓
loader 内部调用 this.addFeatures() 或 callback() 通知数据已加载
```

**当前代码的问题**:

```typescript
loader: async (extent, resolution, projection) => {
  // ...
  const features = new GeoJSON().readFeatures(jsonData, {...})
  source.addFeatures(features)  // ← 此时 loader 可能已经完成执行
  // ...
}
```

当 loader 是 `async` 函数时:
- async 函数返回一个 Promise
- OpenLayers 不会等待 Promise 完成
- 异步代码执行完成后，features 已经被添加到 source，但 OpenLayers 可能已经认为 loader 已经完成了

### 正确的写法

```typescript
// 方案 1: 使用 callback
loader: (extent, resolution, projection) => {
  fetch(url).then(response => response.json()).then(data => {
    const features = new GeoJSON().readFeatures(data)
    source.addFeatures(features)
    source.endLoading()  // 通知加载完成
  }).catch(error => {
    source.removeLoadedExtents(extent)
    source.endLoading()  // 即使失败也要通知
  })
}

// 方案 2: 使用 this.addFeatures() 并返回
loader: (extent, resolution, projection) => {
  fetch(url).then(response => response.text()).then(text => {
    const data = JSON.parse(text)
    const features = new GeoJSON().readFeatures(data)
    return features  // return 给 OpenLayers
  })
}
```

---

## 验证步骤

1. 检查 Network 面板:
   - 查看 `/api/v1/datasets/{id}/geojson` 的响应
   - 确认响应格式是 `{"type":"FeatureCollection"...}` 还是 `{ "data": {...} }`

2. 在浏览器控制台执行:

```javascript
const layers = window.__map__.getAllLayers()
const vectorLayer = layers.find(l => l.get('layerId'))
const source = vectorLayer.getSource()

console.log('Loaded features:', source.getFeatures().length)
console.log('Features:', source.getFeatures())
console.log('Extent:', source.getExtent())
```

3. 如果 features.length 为 0但在 Network 中看到了有效的 GeoJSON，说明问题出在 loader 的异步执行

---

## 修复方案

将 async loader 改为同步方式:

```typescript
const source: any = new VectorSource({
  projection: 'EPSG:4326',
  loader: (extent: any, resolution: any, projection: any) => {
    const token = localStorage.getItem('access_token')
    const url = `/api/v1/datasets/${layerInfo.id}/geojson`

    fetch(url, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        return response.text()
      })
      .then(text => {
        const jsonData = JSON.parse(text)
        const features = new GeoJSON().readFeatures(jsonData, {
          featureProjection: 'EPSG:4326'
        })
        source.addFeatures(features)
        console.log('[MapContainer] Loaded features:', features.length)
      })
      .catch(error => {
        console.error('[MapContainer] Failed to load GeoJSON:', error)
      })
  },
  format: new GeoJSON()
})
```

**关键改动**:
1. 移除 `async` 关键字
2. 使用 `.then()` 链式调用替代 `await`
3. source 在 `.then()` 回调中被填充

---

## 总结

**根本原因**: VectorSource 的 loader 使用了 `async` 函数，导致 OpenLayers 无法正确等待数据加载完成。

**修复**: 将 async/await 改为 Promise.then() 链式调用。
