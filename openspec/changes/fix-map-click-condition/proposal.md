# Proposal: fix-map-click-condition

## Summary

修复 MapContainer.vue 中 Click 变量未定义导致地图页面异常的 bug。

## Problem Statement

根据诊断报告 `openspec/specs/debug-map-layer-list.md`：

Console 报错：
```
ReferenceError: Click is not defined
    at initMap (MapContainer.vue:94:16)
```

代码分析：
- 第 28 行：`import { click as clickCondition } from 'ol/events/condition'`
- 第 94 行：`condition: Click` ← 使用了未定义的变量

导入时使用了别名 `clickCondition`，但使用时报错使用了 `Click`。

由于 MapContainer 组件在 mounted 时报错，导致整个页面无法正常渲染，图层列表显示为空。

## Root Cause

变量名不一致：
- 导入：`click as clickCondition` → 变量名是 `clickCondition`
- 使用：`condition: Click` → 使用了 `Click`（未定义）

## Goals

1. 将 MapContainer.vue 第 94 行的 `Click` 改为 `clickCondition`
2. 确保页面正常渲染

## Success Criteria

- 刷新地图页面无 Console 报错
- 左侧图层列表正常显示已发布数据集
