# Proposal: fix-map-source-projection

## Summary

修复 VectorSource 缺少 projection 配置导致地图自动缩放失败的问题。

## Problem

用户在地图页面勾选已发布的数据集图层后，GeoJSON 数据成功加载（Network 返回 200），但 `map.getView().fit(source.getExtent(), ...)` 没有任何效果，地图视图未自动缩放到数据范围。

### Root Cause (from `specs/debug-zoom-final.md`)

`MapContainer.vue` 中的 VectorSource 构造函数缺少 `projection` 配置:

```typescript
// 当前代码 - 缺少 projection
const source = new VectorSource({
  loader: async (extent, resolution, projection) => {...}
})
```

当 VectorSource 没有显式设置 projection 时:
- `source.getProjection()` 返回 `null`
- loader 回调中的 `projection` 参数不正确
- `source.getExtent()` 在错误的坐标系下计算
- `fit()` 收到无效 extent 或坐标不匹配，导致地图无反应

## Solution

1. **为 VectorSource 添加 projection 配置**: `projection: 'EPSG:4326'`
2. **明确指定 featureProjection**: 使用固定值 `'EPSG:4326'` 替代动态参数

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Function: `loadLayer` (around line 134)

## Success Criteria

- [ ] 勾选图层后，地图自动缩放到数据范围
- [ ] source.getProjection() 返回正确的 projection 对象
- [ ] source.getExtent() 返回有效的 extent 值
