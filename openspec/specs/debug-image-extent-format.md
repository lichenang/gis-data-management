# 影像 extent 格式诊断报告

## 问题描述

影像图层加载后地图仍飞入海里。需要检查 extent 格式是否前后端一致。

## 1. 后端 extent 格式

**文件**: `backend/.../entity/ImageWmsInfo.java`

```java
@Schema(description = "影像覆盖范围 [minX, minY, maxX, maxY]")
private double[] extent;
```

**后端返回格式**: `double[]` - 数组 `[minX, minY, maxX, maxY]`，JSON 序列化为数组如 `[12930000, 4855000, 12940000, 4856000]`

**设置位置**: `backend/.../ImageServiceImpl.java:408-419`

```java
if (transformedExtent != null) {
    info.setExtent(transformedExtent);  // double[] 数组
    // ...
}
```

## 2. 前端 extent 格式

**文件**: `frontend/.../map/MapContainer.vue`

```typescript
interface ImageLayerInfo {
  // ...
  extent?: [number, number, number, number]  // 元组类型
}
```

**前端期望**: `[number, number, number, number]` - 数组格式

## 3. 接口不匹配问题！

### 问题1: LayerPanel 中接口定义

```typescript
// LayerPanel.vue - 本地接口，没有 extent!
interface ImageLayerInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number
  visible: boolean  // 缺失 extent 字段!
}
```

但是数据获取时使用了对象展开：
```typescript
// LayerPanel.vue:178-183
imageLayers.value.push({
  id: d.id!,
  name: d.name || '',
  ...wmsResponse.data,  // 这里会包含 extent!
  visible: false
})
```

所以 extent 实际存在于数据对象中，只是接口类型定义不完整。

### 问题2: index.vue 中接口定义

```typescript
// index.vue:54-61
interface ImageLayerDisplayInfo {
  id: number
  name: string
  wmsUrl: string
  layerName: string
  crs: string
  opacity: number  // 缺失 extent 字段!
}
```

同样，handleImageLayerChange 使用对象展开传递所有属性：
```typescript
.selected
  .map(l => ({
    ...l,  // 展开所有属性包括 extent
    opacity: ...
  }))
```

## 4. 实际数据流程

```
后端 (Java)
    │
    ▼ JSON 序列化
    │ { extent: [12930000,4855000,12940000,4856000], crs: "EPSG:3857", ... }
    │
前端 (浏览器)
    ▼
getImageWmsUrl() 返回
    │
    ▼ LayerPanel.vue
imageLayers.value.push({ 
  ...wmsResponse.data  // 包含 extent 数组!
})
    │
    ▼ emit('image-layer-change', selected)
    │
index.vue handleImageLayerChange()
    │
    ▼ selectedImageLayers = layers
    │
MapContainer :image-layers="selectedImageLayers"
    │
    ▼
imageInfo.extent 应该是数组 [12930000,4855000,12940000,4856000]
```

## 5. 结论：格式应该一致

Extent 格式应该是正确的 `[number, number, number, number]` 数组格式。

## 6. 请在浏览器控制台验证

请在浏览器开发者工具中执行以下验证：

### A. 检查 API 返回的实际数据

打开 Network 面板，找到 `/api/v1/images/{id}/wms-url` 请求，点击 Response 查看：

```json
{
  "code": 200,
  "data": {
    "wmsUrl": "...",
    "layerName": "raster_XX",
    "crs": "EPSG:3857",
    "opacity": 0.8,
    "extent": [12930000, 4855000, 12940000, 4856000]  // ← 确认是数组
  }
}
```

### B. 在 MapContainer loadImageLayer 中添加调试

在 `frontend/.../MapContainer.vue` 的 `loadImageLayer` 函数开头添加：

```typescript
console.log('[loadImageLayer] imageInfo:', JSON.stringify(imageInfo))
console.log('[loadImageLayer] extent:', imageInfo.extent, Array.isArray(imageInfo.extent))
console.log('[loadImageLayer] extent values:', 
  imageInfo.extent?.[0], 
  imageInfo.extent?.[1], 
  imageInfo.extent?.[2], 
  imageInfo.extent?.[3]
)
```

### C. 检查后端日志

查看后端控制台日志，应该有类似输出：

```
Image 27 extent transformation: sourceCRS=EPSG:3857, extent=[12930000.0, 4855000.0, 12940000.0, 4856000.0]
Successfully transformed extent to EPSG:4326: [116.xxxx, 39.xxxx, 116.xxxx, 40.xxxx]
```

如果看到 `Failed to transform extent` 或 `sourceCRS` 不是 `EPSG:3857`，说明 CRS 转换未成功。

## 7. 可能的问题原因

| 症状 | 原因 |
|------|------|
| extent 是米制坐标但没转换 | 条件 `isView4326 && !isGeographicExtent` 不满足 |
| extent 是 undefined/null | 数据传递过程中丢失 |
| extent 是乱码 | JSON 序列化/反序列化问题 |

## 8. 快速修复建议

如果确认 extent 格式正确但仍有问题，可以在 loadImageLayer 中强制假设：

```typescript
// 强制假设 extent 是米制坐标，需要转换
// 因为 GeoServer 影像通常是 EPSG:3857
import('ol/proj').then((proj) => {
  // 强制转换，不管 crs 是什么
  const transformed = proj.transformExtent(
    extent, 
    'EPSG:3857',  // 强制假设源 CRS
    'EPSG:4326'
  )
  view.fit(transformed, {...})
})
```
