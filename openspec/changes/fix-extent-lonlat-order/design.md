# Design: fix-extent-lonlat-order

## 问题分析

OpenLayers `view.fit(extent)` 要求 extent 格式为 `[minLon, minLat, maxLon, maxLat]`。

当前代码中 CrsTransformUtil 的 result 数组构建逻辑：

```java
double[] result = new double[4];
result[0] = minResult[0];  // minLon
result[1] = minResult[1];  // minLat
result[2] = maxResult[0];  // maxLon
result[3] = maxResult[1];  // maxLat
```

## 修复方案

确认 `CrsTransformUtil.transformExtentToWgs84()` 返回正确的 `[minLon, minLat, maxLon, maxLat]` 顺序。

### 检查点

1. **输入**: `extent = [minX, minY, maxX, maxY]` (EPSG:3857 米制坐标)
2. **minPoint**: `new double[]{extent[0], extent[1]}` = `[minX, minY]`
3. **maxPoint**: `new double[]{extent[2], extent[3]}` = `[maxX, maxY]`
4. **转换后**:
   - `minResult = [minLon, minLat]`
   - `maxResult = [maxLon, maxLat]`
5. **输出**: `result = [minLon, minLat, maxLon, maxLat]` ✓

### 当前代码状态

当前代码已经返回正确顺序 `[minLon, minLat, maxLon, maxLat]`。

如果诊断报告显示问题是由于数组顺序错误，需要进一步检查：
1. dataset.extent JSON 中 minX/minY 是否对应正确的经度/纬度
2. GeoTiffParser 中 envelope.getMinimum(0) 和 getMinimum(1) 的含义

## 验证

修复后日志应显示：
```
Transformed extent from EPSG:3857 to EPSG:4326: [12930000.000000, 4855000.000000, 12940000.000000, 4856000.000000] -> [108.943521, 39.856, 109.943521, 39.856]
```

输出的 extent 应为 `[108.94, 39.86, 109.94, 39.86]` 格式。
