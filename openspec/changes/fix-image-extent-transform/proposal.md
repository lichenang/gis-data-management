# Proposal: fix-image-extent-transform

## 问题描述

影像图层加载后，地图跳转到海里（错误位置），且影像瓦片请求范围不正确，导致影像无法显示。

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  修复 fix-image-crs-return-value 后：                                       │
│  ├─ 后端正确返回 crs: "EPSG:3857" (GeoServer 实际 CRS)                     │
│  └─ 后端正确返回 extent: EPSG:4326 坐标 (已转换)                           │
│                                                                             │
│  但前端仍然有问题:                                                          │
│                                                                             │
│  后端返回: extent = [lon1, lat1, lon2, lat2] (EPSG:4326)                   │
│           crs = "EPSG:3857"                                                 │
│                                                                             │
│  前端 loadImageLayer():                                                     │
│  ├─ fit(extent) 直接使用后端返回的 extent                                   │
│  │   ✓ 此时 extent 已是 EPSG:4326，应该可以直接用                         │
│  │   ✗ 但如果转换失败，extent 仍是 EPSG:3857                              │
│  │                                                                         │
│  ├─ TileWMS 错误: projection 使用 imageInfo.crs="EPSG:3857"               │
│  │   ✗ GeoServer WMS 默认使用 EPSG:4326 响应                              │
│  │   ✓ 需要让 OpenLayers 自动处理: projection 不指定或设为数据 CRS       │
│  │                                                                         │
│  实际问题是:                                                                │
│  1. 后端extent转换可能失败（dataset.srs 格式不对）                         │
│  2. 前端没有根据 crs 做好容错：                                             │
│     - 如果 crs 是 "EPSG:3857"，extent 需要动态转换                        │
│     - 如果 crs 是 "EPSG:4326"，直接使用                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

### 关键思路

**后端返回的 extent 不可靠**（转换可能失败），前端必须自己根据 `imageInfo.crs` 决定是否转换：

1. 如果 `imageInfo.crs === 'EPSG:3857'` 且视图是 `EPSG:4326`：
   - 使用 `transformExtent` 动态转换 extent
2. 如果 `imageInfo.crs === 'EPSG:4326'` 或没有 crs：
   - 直接使用 extent（后端已转换）

### TileWMS projection 处理

GeoServer WMS 对于非 EPSG:4326 数据，默认行为可能不一致。更好的做法是：
- 不指定 projection，让 OpenLayers 自动推断
- 或者指定与数据相同的 projection

## 影响范围

- 修改: `frontend/src/views/map/MapContainer.vue` - loadImageLayer() 函数

## 风险评估

- 低风险：仅修改前端 extent 转换逻辑
