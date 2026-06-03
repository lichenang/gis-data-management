# Proposal: fix-map-dom-structure

## Summary

修复地图页面 DOM 结构和 CSS 布局导致的高度不足问题。

## Problem

地图页面高度反复修复仍无效，实际渲染高度不足屏幕的三分之一。

### Root Cause (from `specs/debug-map-height-final.md`)

1. **多余的闭合标签**: index.vue 第 35-36 行存在多余的 `</div>`，破坏 DOM 结构
2. **el-container 缺少高度**: `<el-container>` 需要显式 `height: 100%`
3. **flex 子元素未设置高度**: `.map-main` 内的 MapContainer 需要 `height: 100%`

## Solution

1. 删除多余的 `</div>` 闭合标签
2. 为 el-container 添加 `height: 100%`
3. 为 map-main 子元素添加 `height: 100%`

## Scope

- File: `frontend/src/views/map/index.vue`

## Success Criteria

- [ ] 地图占满视口剩余高度
- [ ] 页面无滚动条
- [ ] 矢量图层样式正常显示
