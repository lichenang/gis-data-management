# Proposal: add-map-breadcrumb

## Summary

为地图查看页面添加面包屑导航条，支持用户快速返回首页。

## Problem

当前地图页面 (`/map`) 缺少导航指示，用户进入地图页面后无法快速返回首页。

## Solution

在地图页面顶部添加面包屑导航:
- 显示: "首页 / 地图查看"
- "首页" 为可点击链接，点击跳转至 `/`
- 面包屑背景色与页面风格协调
- 不影响地图主体的全屏高度计算

## Scope

- File: `frontend/src/views/map/index.vue`

## Success Criteria

- [ ] 面包屑显示在页面顶部
- [ ] "首页" 为可点击链接
- [ ] 面包屑样式与页面协调
- [ ] 地图区域高度不受影响
