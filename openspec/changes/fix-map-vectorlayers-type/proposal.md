# Proposal: fix-map-vectorlayers-type

## Summary

修复 MapContainer.vue 中 vectorLayers 使用 Map 导致 .has() 调用失败的问题。

## Problem Statement

Console 报错：
```
MapContainer.vue:168 - vectorLayers.value.has is not a function
```

代码中 vectorLayers 初始化为 `ref<Map<number, VectorLayerType>>(new Map())`，但 Vue 3 的响应式 ref 对原生 Map 方法支持不佳。

## Root Cause

Vue 3 `ref()` 对复杂对象（Map、Set）的响应式处理不完整，调用 `.has()` 时 vectorLayers.value 不是真正的 Map 对象。

## Goals

1. 将 vectorLayers 从 Map 改为普通对象
2. 修复所有相关的 .has()、.set()、.get()、.delete() 调用

## Success Criteria

- 刷新地图页面无报错
- 勾选图层后地图显示要素
