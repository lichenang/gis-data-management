# Proposal: fix-extent-lonlat-order

## 问题描述

后端 `getImageWmsInfo()` 返回的 extent 数组顺序错误，导致地图中心跳转到错误位置。

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  OpenLayers view.fit() 期望 extent 格式:                                   │
│  [minLon, minLat, maxLon, maxLat]                                          │
│                                                                             │
│  当前后端 CrsTransformUtil 返回:                                            │
│  [minResult[0], minResult[1], maxResult[0], maxResult[1]]                  │
│  = [minLon, minLat, maxLon, maxLat] ✓                                      │
│                                                                             │
│  但 fallback 方法 transformExtentSimple 返回:                               │
│  [minLon, minLat, maxLon, maxLat]                                          │
│                                                                             │
│  可能的问题: CrsTransformUtil 中的坐标提取顺序有误                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

修改 `CrsTransformUtil.transformExtentToWgs84()` 方法，确保返回正确的 `[minLon, minLat, maxLon, maxLat]` 顺序。

## 影响范围

- `backend/.../CrsTransformUtil.java` - 修正 extent 数组构建顺序

## 风险评估

- 低风险：仅调整数组索引顺序，不影响转换逻辑
