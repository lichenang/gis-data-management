# Proposal: fix-final-extent-lonlat-order

## 问题描述

影像图层加载后地图不显示，地图中心跳转到错误位置。

## 根因分析

API 返回的 extent 数组顺序与 OpenLayers 期望不一致：

| 来源 | 格式 | 示例 |
|------|------|------|
| API 返回 | `[minLat, minLon, maxLat, maxLon]` | `[34.16, 108.96, 34.17, 108.97]` |
| OpenLayers 期望 | `[minLon, minLat, maxLon, maxLat]` | `[108.96, 34.16, 108.97, 34.17]` |

### 数据流分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        数据流分析                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  数据库 extent JSON:                                                        │
│  {"minX": 34.16, "minY": 108.96, "maxX": 34.17, "maxY": 108.97}           │
│       │                                                                      │
│       │  JSON 字段名: minX, minY, maxX, maxY                                │
│       │  实际存储值: 34.16=lat, 108.96=lon (顺序错误!)                       │
│       │                                                                      │
│       ▼                                                                      │
│  ImageServiceImpl 解析 (line 396-399):                                      │
│  extent[0] = minX = 34.16   (实际是 minLat!)                               │
│  extent[1] = minY = 108.96  (实际是 minLon!)                               │
│  extent[2] = maxX = 34.17   (实际是 maxLat!)                               │
│  extent[3] = maxY = 108.97  (实际是 maxLon!)                               │
│       │                                                                      │
│       ▼                                                                      │
│  CrsTransformUtil 转换 (假设输入是 lon/lat):                                │
│  minPoint = [34.16, 108.96] ← 被当作 lon/lat 但实际是 lat/lon!              │
│       │                                                                      │
│       ▼                                                                      │
│  输出: [34.16, 108.96, 34.17, 108.97]                                       │
│       │                                                                      │
│       ▼                                                                      │
│  前端 fit([34.16, 108.96, 34.17, 108.97])                                   │
│       │                                                                      │
│       ▼                                                                      │
│  地图跳转到: 34.16°N, 108.96°E 附近                                         │
│       │                                                                      │
│       ▼                                                                      │
│  但影像实际位置是西安附近 (34.2°N, 108.9°E)                                 │
│       │                                                                      │
│       ▼                                                                      │
│  如果坐标被误读为 lon/lat 而非 lat/lon，会导致显示错误                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

在 `ImageServiceImpl.getImageWmsInfo()` 方法中，交换 extent 数组的构建顺序：

```java
// 当前代码 (错误):
extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // minLat
extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // minLon
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();  // maxLat
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();  // maxLon

// 修复后 (正确):
// JSON 存储的是 minX=lat, minY=lon，需要交换为 lon/lat 顺序
extent[0] = ((Number) extentMap.get("minY")).doubleValue();  // minLon = 108.96
extent[1] = ((Number) extentMap.get("minX")).doubleValue();  // minLat = 34.16
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();  // maxLon = 108.97
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();  // maxLat = 34.17
```

## 影响范围

- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

## 风险评估

- 低风险：仅调整数组索引映射，不影响 GeoTools 转换逻辑
