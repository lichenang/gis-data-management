# 设计方案：修复 extent 坐标轴顺序

## 问题分析

### 数据流

```
数据库 extent 字段
    │
    ▼
ImageServiceImpl.getImageWmsInfo()
    │ extent 格式: {minX, minY, maxX, maxY}
    │
    ▼ 396-399行错误解析
CrsTransformUtil.transformExtentToWgs84()
    │ 假设输入: [lon1, lat1, lon2, lat2]
    │ 实际输入: [minY, minX, maxY, maxX]  ← 错误的 XY 顺序
    │
    ▼
返回错误的 EPSG:4326 坐标
    │
    ▼
前端接收错误的 extent → 地图无法正确定位
```

### 修复方案

修改 `ImageServiceImpl.java:396-399`，正确解析 extent：

```java
// 错误代码
extent[0] = ((Number) extentMap.get("minY")).doubleValue();
extent[1] = ((Number) extentMap.get("minX")).doubleValue();
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();

// 正确代码
extent[0] = ((Number) extentMap.get("minX")).doubleValue();
extent[1] = ((Number) extentMap.get("minY")).doubleValue();
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();
```

## 修改位置

- 文件: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
- 行号: 396-399
