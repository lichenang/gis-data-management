# 设计方案：修复 CrsTransformUtil 坐标轴交换

## 数据流转换

```
输入: [minX, minY, maxX, maxY] (EPSG:3857 米制)
  ↓
minPoint = {extent[0], extent[1]} = {minX, minY} = {lon, lat}
maxPoint = {extent[2], extent[3]} = {maxX, maxY} = {lon, lat}
  ↓
GeoTools 转换 (EPSG:3857 → EPSG:4326):
minResult = {lat, lon}  ← 轴顺序按 EPSG:4326 定义
maxResult = {lat, lon}
  ↓
当前代码 (错误):
result[0] = minResult[0] = lat
result[1] = minResult[1] = lon
result[2] = maxResult[0] = lat
result[3] = maxResult[1] = lon
→ 输出: [lat, lon, lat, lon] ✗

修复后 (正确):
result[0] = minResult[1] = lon
result[1] = minResult[0] = lat
result[2] = maxResult[1] = lon
result[3] = maxResult[0] = lat
→ 输出: [lon, lat, lon, lat] ✓
```

## 修改位置

- 文件: `backend/src/main/java/com/gisplatform/util/CrsTransformUtil.java`
- 行号: 52-55
