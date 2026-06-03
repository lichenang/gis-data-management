# Proposal: fix-geojson-geometry-type

## Summary

修复 `DatasetServiceImpl.getDatasetAsGeoJSON()` 方法中 geometry.type 被硬编码为无效值 "Geometry" 的问题。

## Problem Statement

根据诊断报告 `openspec/specs/debug-map-empty.md`，地图查看页面无法显示已发布数据集的根本原因是：

1. 后端 GeoJSON 接口返回的 geometry.type 是 "Geometry"（无效值）
2. GeoJSON 规范要求 geometry.type 必须是具体类型：Point、LineString、Polygon、MultiPoint、MultiLineString、MultiPolygon
3. OpenLayers 的 GeoJSON 解析器无法识别 "Geometry"，导致要素不被渲染

## Root Cause

```java
// 错误代码 (DatasetServiceImpl.java 第 160 行)
Map<String, Object> geometry = new HashMap<>();
geometry.put("type", "Geometry");  // ← 硬编码了无效的几何类型
geometry.put("coordinates", parseGeoJSONCoordinates(geojsonObj.toString()));
```

`ST_AsGeoJSON(geom)` 返回的 JSON 已经包含了正确的 type 字段，但代码没有使用，而是手动构建了无效的 geometry 对象。

## Goals

1. 修改 `getDatasetAsGeoJSON()` 方法，直接使用 `ST_AsGeoJSON` 返回的原始 JSON
2. 确保返回的 GeoJSON 包含正确的 geometry.type
3. 验证修复后前端能正常渲染地图要素

## Success Criteria

- GET /api/v1/datasets/{id}/geojson 返回的 geometry.type 是具体类型（如 "Point"）
- 前端地图能正常显示已发布数据集的要素
