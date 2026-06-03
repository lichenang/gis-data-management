# fix-crs-transform-axis-swap

## 问题描述

`CrsTransformUtil.transformExtentToWgs84()` 返回的坐标顺序与 GIS 标准实践不一致，且与备用方法 `transformExtentSimple()` 输出顺序矛盾。

## 问题根因

### 1. EPSG:4326 轴顺序问题

EPSG:4326 官方定义的轴顺序是 **(纬度, 经度)** = **(lat, lon)**，这与常见的 **(经度, 经度)** = **(lon, lat)** 顺序相反。

GeoTools 在转换时遵循 CRS 的轴顺序定义：
- 输入 EPSG:3857 (lon, lat) → 输出 EPSG:4326 (lat, lon)

### 2. 代码中的问题

**CrsTransformUtil.java:52-55** - 主方法输出顺序错误：
```java
result[0] = minResult[0];  // 错误：minResult[0] = lat，应该是 lon
result[1] = minResult[1];  // 错误：minResult[1] = lon，应该是 lat
result[2] = maxResult[0];  // 错误：maxResult[0] = lat，应该是 lon
result[3] = maxResult[1];  // 错误：maxResult[1] = lon，应该是 lat
```

**ImageServiceImpl.java:444-447** - 备用方法输出顺序（正确但与主方法不一致）：
```java
result[0] = Math.toDegrees(minX_rad);              // lon ✓
result[1] = Math.toDegrees(2 * minY_rad - PI / 2); // lat ✓
result[2] = Math.toDegrees(maxX_rad);              // lon ✓
result[3] = Math.toDegrees(2 * maxY_rad - PI / 2); // lat ✓
```

## 影响分析

| 场景 | GeoTools 转换 | 输出顺序 | 是否正确 |
|------|--------------|---------|---------|
| GeoTools 成功 | 使用 CRS.findMathTransform | [lat, lon, lat, lon] | ✗ |
| GeoTools 失败 | 使用 transformExtentSimple | [lon, lat, lon, lat] | ✓ |

这导致行为不一致：
- 当 GeoTools 工作时 → 返回错误顺序
- 当 GeoTools 失败时 → 返回正确顺序（但数值可能错误）

## 修复方案

### 1. 修复 CrsTransformUtil.transformExtentToWgs84()

修改 `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java` 第 52-55 行：

```java
// 修改前
result[0] = minResult[0];  // lat
result[1] = minResult[1];  // lon
result[2] = maxResult[0];  // lat
result[3] = maxResult[1];  // lon

// 修改后
result[0] = minResult[1];  // lon
result[1] = minResult[0];  // lat
result[2] = maxResult[1];  // lon
result[3] = maxResult[0];  // lat
```

### 2. 验证修复

修复后，输出顺序应统一为 **[lon, lat, lon, lat]**：

```
输入: [minX, minY, maxX, maxY] (EPSG:3857 米制)
  ↓
minPoint = {minX, minY} = {lon, lat}
maxPoint = {maxX, maxY} = {lon, lat}
  ↓
GeoTools 转换后:
minResult = {lat, lon}
maxResult = {lat, lon}
  ↓
交换后输出:
result[0] = minResult[1] = minLon ✓
result[1] = minResult[0] = minLat ✓
result[2] = maxResult[1] = maxLon ✓
result[3] = maxResult[0] = maxLat ✓
  ↓
输出: [minLon, minLat, maxLon, maxLat] (EPSG:4326)
```

## 修改位置

| 文件 | 行号 | 修改内容 |
|------|------|---------|
| `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java` | 52-55 | 交换 minResult/maxResult 的索引赋值顺序 |
