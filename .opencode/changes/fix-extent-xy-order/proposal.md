# fix-extent-xy-order

## 问题描述

影像图层定位失败，地图没有跳转到正确位置（西安），且 WMS 请求返回灰白图片。

## 根本原因

`ImageServiceImpl.java` 第 396-399 行存在 extent 坐标轴顺序错误。

数据库存储的 extent 格式为：
```json
{minX: 12129263.4322, minY: 4050247.7206, maxX: 12130944.1474, maxY: 4051356.3316}
```

当前代码错误地交换了 XY 顺序：
```java
extent[0] = ((Number) extentMap.get("minY")).doubleValue();  // 错误：应该是 minX
extent[1] = ((Number) extentMap.get("minX")).doubleValue();  // 错误：应该是 minY
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();  // 错误：应该是 maxX
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();  // 错误：应该是 maxY
```

这导致：
1. 传给 GeoTools 坐标转换的是 `(minY, minX)` 而不是 `(minX, minY)`
2. 转换后的 WGS84 坐标完全错误
3. 前端接收到错误的 extent，地图无法跳转到正确位置
4. WMS 请求使用了错误的 CRS 和 extent 参数

## 预期结果

修复后：
1. extent 坐标正确传递给 GeoTools 进行转换
2. 前端接收到正确的 EPSG:4326 坐标
3. 地图能够跳转到影像所在位置（西安）
4. WMS 请求返回正确的影像数据
