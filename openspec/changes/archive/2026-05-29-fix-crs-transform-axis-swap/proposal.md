# fix-crs-transform-axis-swap

## 问题描述

`CrsTransformUtil.transformExtentToWgs84()` 返回的坐标顺序与 GIS 标准实践不一致，且与备用方法 `transformExtentSimple()` 输出顺序矛盾。

## 根本原因

EPSG:4326 官方定义的轴顺序是 **(纬度, 经度)** = **(lat, lon)**，但常见的 GIS 约定是 **(经度, 纬度)** = **(lon, lat)**。

GeoTools 在转换时遵循 CRS 的轴顺序定义：
- 输入 EPSG:3857: `(lon, lat)`
- 输出 EPSG:4326: `(lat, lon)`

当前代码 (CrsTransformUtil.java:52-55) 直接将 GeoTools 返回的 `(lat, lon)` 赋值给 result 数组，导致输出顺序错误 `[lat, lon, lat, lon]`。

## 影响分析

| 场景 | GeoTools 转换 | 输出顺序 | 是否正确 |
|------|--------------|---------|---------|
| GeoTools 成功 | CRS.findMathTransform | [lat, lon, lat, lon] | ✗ |
| GeoTools 失败 | transformExtentSimple | [lon, lat, lon, lat] | ✓ |

行为不一致导致调试困难。

## 预期结果

修复后，`transformExtentToWgs84()` 输出统一的 **[lon, lat, lon, lat]** 顺序。
