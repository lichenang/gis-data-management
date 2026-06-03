# Proposal: fix-map-extent-validation

## Summary

修复地图自动缩放失败的问题。用户在地图页面勾选已发布的数据集图层后，地图未自动缩放到数据范围。

## Problem

GeoJSON 数据正常加载，但 `setTimeout` 内的 `fit()` 调用未生效，地图视图未自动调整。

### Root Cause (from `specs/debug-zoom-root-cause.md`)

当前 `MapContainer.vue:160` 的 extent 检查逻辑存在缺陷:

```typescript
if (extent && !isNaN(extent[0]) && !isNaN(extent[1]))
```

只检查了 `extent` 数组的前两个值 (minX, minY)，未检查后两个值 (maxX, maxY)。

当数据满足以下条件时会失败:
- API 返回空 FeatureCollection → extent 为 `[Infinity, Infinity, -Infinity, -Infinity]`
- 数据只有单个点要素 → extent 部分值为 NaN
- 异常数据 → extent 包含 Infinity 值

此时检查通过但 `fit()` 收到无效参数。

## Solution

1. **完善 extent 边界检查**: 使用 `isFinite()` 检查所有 4 个坐标值
2. **处理空数据场景**: 检测无效 extent 并记录警告日志
3. **防御性编程**: 为 projection 参数设置默认值

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Function: `loadLayer` (lines 130-186)

## Success Criteria

- [ ] 勾选单个图层时，地图自动缩放到数据范围
- [ ] 勾选多个图层时，地图自动缩放到所有数据的合并范围
- [ ] 空数据或无效数据时不再报错，优雅降级
