# Proposal: fix-map-full-height

## Summary

修复添加面包屑导航后地图高度又缩回约 1/3 视口的问题。

## Problem

添加面包屑后，地图区域高度计算错误，仅占约 1/3 视口。用户反馈：
- 面包屑栏高度未精确计算
- 地图区域高度不足

## Solution

1. 面包屑栏高度固定 40px (包含 padding)
2. 地图区域高度改为 `calc(100vh - 100px)` - 减去顶栏(60px) + 面包屑(40px)
3. 确保页面整体无滚动条
4. 保留之前的 polish-map-ui 样式（橙色矢量、缩放按钮）

## Scope

- File: `frontend/src/views/map/index.vue`
- File: `frontend/src/views/map/MapContainer.vue` (如需检查)

## Success Criteria

- [ ] 地图区域占满视口剩余高度
- [ ] 页面无滚动条
- [ ] 矢量图层样式为橙色 (已保持)
- [ ] 缩放按钮功能正常 (已保持)
