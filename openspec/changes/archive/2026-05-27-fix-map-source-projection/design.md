# Design: fix-map-source-projection

## Overview

在 `MapContainer.vue` 的 `loadLayer` 函数中，为 VectorSource 显式设置 projection，确保 extent 计算和 fit 操作正确工作。

## Current Code

```typescript
// MapContainer.vue:134-177
const source: any = new VectorSource({
  loader: async (extent: any, resolution: any, projection: any) => {
    // ...
    const features = new GeoJSON().readFeatures(text, {
      featureProjection: projection || 'EPSG:4326'
    })
    // ...
  },
  format: new GeoJSON()
})
```

## Changes

### Change 1: 添加 VectorSource projection 配置

在 VectorSource 构造函数中添加 `projection: 'EPSG:4326'`:

```typescript
const source: any = new VectorSource({
  projection: 'EPSG:4326',  // 新增
  loader: async (extent: any, resolution: any, projection: any) => {
    // ...
  },
  format: new GeoJSON()
})
```

**理由**: 
- 明确告知 VectorSource 数据使用的坐标系
- 确保 extent 计算使用正确的投影
- 使 `source.getProjection()` 返回有效的 projection 对象

### Change 2: 明确指定 featureProjection

将动态参数改为固定值:

```typescript
const features = new GeoJSON().readFeatures(text, {
  featureProjection: 'EPSG:4326'  // 改为固定值
})
```

**理由**:
- 避免 loader 参数 projection 为 null 时的问题
- 明确数据将转换到的目标坐标系
- 使代码更清晰易读

## Implementation Notes

- 这是 OpenLayers 的最佳实践：始终为 VectorSource 设置 projection
- 即使 View 的 projection 已知，也需要在 Source 中明确指定
- 修改后可在浏览器控制台验证: `source.getProjection().getCode()` 应返回 `'EPSG:4326'`
