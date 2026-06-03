# Proposal: fix-map-loadlayer-timing

## Summary

修复 MapContainer 组件因初始化时序问题导致的矢量图层无法添加到地图的 bug。

## Problem

用户勾选地图图层后，矢量图层未被添加到地图，`map.getAllLayers()` 仅返回底图图层（1个）。

### Root Cause (from `specs/debug-vectorlayer-not-added.md`)

组件初始化时序问题:

1. 组件创建时 `watch` 立即执行，此时 `map.value` 为 `null`
2. 如果用户立即勾选图层，`loadLayer()` 会被调用但因 `map.value` 为空而静默返回
3. 虽然通常场景下用户是在页面加载完成后才勾选，但 watch/loadLayer 的时序仍存在问题

## Solution

1. **在 `onMounted` 中 initMap 后重新加载已选中的图层**: 确保 map 初始化完成后加载所有已选中的图层
2. **添加调试日志**: 便于后续排查类似问题

## Scope

- File: `frontend/src/views/map/MapContainer.vue`
- Functions: `initMap`, `loadLayer`

## Success Criteria

- [ ] watch 触发后矢量图层正确添加到地图
- [ ] 控制台显示加载日志
- [ ] 地图包含底图 + 所有已选中的矢量图层
