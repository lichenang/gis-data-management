# Map GeoJSON 请求 403 问题 - 诊断报告

## 问题描述

地图页面请求 `GET /api/v1/datasets/2/geojson` 返回 403 Forbidden。

## 根因分析

### 代码检查

**VectorSource 加载 GeoJSON** (MapContainer.vue 第 133-135 行):

```typescript
const source = new VectorSource({
  url: `/api/v1/datasets/${layerInfo.id}/geojson`,
  format: new GeoJSON()
})
```

### 问题

OpenLayers 的 `VectorSource` 内部使用 `fetch` API 发起 HTTP 请求，**不会经过 axios 请求拦截器**，因此：

1. 请求不带 JWT Token
2. 后端返回 403 Forbidden

### 对比

| 请求 | 方式 | JWT Token | 结果 |
|------|------|-----------|------|
| `/datasets/published` | axios (via getPublishedDatasets) | ✅ 有 | 200 OK |
| `/datasets/{id}/geojson` | VectorSource (fetch) | ❌ 无 | 403 Forbidden |

## 修复方案

### 方案 1: 使用 Loader 函数（推荐）

替换 URL 为自定义 loader 函数，手动使用 axios 发起请求：

```typescript
const source = new VectorSource({
  loader: async (extent, resolution, projection) => {
    const response = await fetch(`/api/v1/datasets/${layerInfo.id}/geojson`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('access_token')}`
      }
    })
    const text = await response.text()
    const geojson = new GeoJSON().readFeatures(text)
    source.addFeatures(geojson)
  },
  format: new GeoJSON()
})
```

### 方案 2: 使用 axios 手动加载

在 loadLayer 函数中用 axios 请求后转 feature:

```typescript
async function loadLayer(layerInfo: LayerInfo) {
  if (!map.value) return

  const response = await axios.get(`/api/v1/datasets/${layerInfo.id}/geojson`, {
    baseURL: ''  // Use full URL
  })
  
  const features = new GeoJSON().readFeatures(response.data)
  
  const source = new VectorSource()
  source.addFeatures(features)
  
  // ... rest of the code
}
```

## 修改的文件

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `MapContainer.vue` | 133-135 | 将 `url:` 替换为 `loader:` 函数 |

## 验证步骤

1. 修改 MapContainer.vue 使用带 JWT 的加载方式
2. 重启前端
3. 刷新地图页面
4. 勾选图层，验证 geojson 请求成功
5. 验证地图显示要素
