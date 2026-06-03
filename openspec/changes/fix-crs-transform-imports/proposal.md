# Proposal: fix-crs-transform-imports

## Summary

修复 CrsTransformUtil.java 中 GeoTools 32.x 的 import 路径错误，使用正确的 `org.geotools.api.*` 包路径，并移除不存在的 `DirectPosition2D` 改用坐标数组直接转换。

## Problem Statement

GeoTools 32.x 进行了模块化重构，包路径从 `org.opengis.*` 改为 `org.geotools.api.*`。当前 CrsTransformUtil.java 使用了错误的 import 路径，导致编译失败。

## Root Cause

```
GeoTools 32.x 之前:
- org.opengis.referencing.crs.CoordinateReferenceSystem
- org.opengis.referencing.operation.MathTransform
- org.geotools.geometry.DirectPosition2D

GeoTools 32.x (当前使用):
- org.geotools.api.referencing.crs.CoordinateReferenceSystem
- org.geotools.api.referencing.operation.MathTransform
- DirectPosition2D 已移至/废弃
```

## Goals

1. 更新所有 import 到 `org.geotools.api.*` 路径
2. 移除 DirectPosition2D，使用 double[] 数组直接进行坐标转换

## Affected Files

- `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`
