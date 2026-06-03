# Design: fix-final-extent-lonlat-order

## 修复方案

### 文件: ImageServiceImpl.java

**位置**: 第 395-399 行

**修改前**:
```java
double[] extent = new double[4];
extent[0] = ((Number) extentMap.get("minX")).doubleValue();  // minLat
extent[1] = ((Number) extentMap.get("minY")).doubleValue();  // minLon
extent[2] = ((Number) extentMap.get("maxX")).doubleValue();  // maxLat
extent[3] = ((Number) extentMap.get("maxY")).doubleValue();  // maxLon
```

**修改后**:
```java
double[] extent = new double[4];
extent[0] = ((Number) extentMap.get("minY")).doubleValue();  // minLon (原 minY)
extent[1] = ((Number) extentMap.get("minX")).doubleValue();  // minLat (原 minX)
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();  // maxLon (原 maxY)
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();  // maxLat (原 maxX)
```

## 修改说明

| 数组索引 | 修改前 (字段) | 修改后 (字段) | 说明 |
|----------|---------------|---------------|------|
| extent[0] | minX → 34.16 (minLat) | minY → 108.96 (minLon) | 交换 |
| extent[1] | minY → 108.96 (minLon) | minX → 34.16 (minLat) | 交换 |
| extent[2] | maxX → 34.17 (maxLat) | maxY → 108.97 (maxLon) | 交换 |
| extent[3] | maxY → 108.97 (maxLon) | maxX → 34.17 (maxLat) | 交换 |

## 修复后数据流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        修复后数据流                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  数据库 extent JSON:                                                        │
│  {"minX": 34.16, "minY": 108.96, "maxX": 34.17, "maxY": 108.97}           │
│       │                                                                      │
│       ▼                                                                      │
│  ImageServiceImpl 解析 (修复后):                                            │
│  extent[0] = minY = 108.96 → minLon ✓                                      │
│  extent[1] = minX = 34.16 → minLat ✓                                       │
│  extent[2] = maxY = 108.97 → maxLon ✓                                      │
│  extent[3] = maxX = 34.17 → maxLat ✓                                       │
│       │                                                                      │
│       ▼                                                                      │
│  CrsTransformUtil 转换:                                                     │
│  minPoint = [108.96, 34.16] = [minLon, minLat] ✓                           │
│  maxPoint = [108.97, 34.17] = [maxLon, maxLat] ✓                           │
│       │                                                                      │
│       ▼                                                                      │
│  输出: [108.96, 34.16, 108.97, 34.17] = [minLon, minLat, maxLon, maxLat]   │
│       │                                                                      │
│       ▼                                                                      │
│  前端 fit([108.96, 34.16, 108.97, 34.17])                                   │
│       │                                                                      │
│       ▼                                                                      │
│  地图正确缩放到: 108.96°E, 34.16°N 附近 (西安)                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 完整代码修改

### ImageServiceImpl.java (line 395-399)

```java
double[] extent = new double[4];
extent[0] = ((Number) extentMap.get("minY")).doubleValue();  // minLon
extent[1] = ((Number) extentMap.get("minX")).doubleValue();  // minLat
extent[2] = ((Number) extentMap.get("maxY")).doubleValue();  // maxLon
extent[3] = ((Number) extentMap.get("maxX")).doubleValue();  // maxLat
```

## 验证步骤

1. Maven 编译后端
2. 调用 `/api/v1/images/{id}/wms-url` API
3. 检查返回的 extent 数组:
   - extent[0] 应该是 ~108.96 (经度)
   - extent[1] 应该是 ~34.16 (纬度)
   - extent[2] 应该是 ~108.97 (经度)
   - extent[3] 应该是 ~34.17 (纬度)
4. 在地图上添加影像图层，验证地图正确缩放到西安附近
