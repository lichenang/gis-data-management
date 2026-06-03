# Proposal: fix-map-auto-zoom

## Summary

修复地图加载矢量图层后不自动缩放到数据范围的问题。

## Problem Statement

当前 MapContainer.vue 成功请求 GeoJSON 并创建了 VectorLayer，但地图视图没有自动定位到数据范围。用户需要手动缩放和平移才能看到数据。

## Goals

在每次新增 VectorLayer 并加载完 features 后，自动将地图视图缩放到该图层的全图范围。

## Success Criteria

- 勾选图层后，地图自动缩放到数据范围
- 设置合适的 padding 和 maxZoom，确保用户体验
