# Proposal: fix-image-extent-final

## 问题描述

影像图层加载后地图跳入海里（西非附近），影像不显示。

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        多次修复未根除                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  修复历史:                                                                  │
│  1. fix-srs-epsg-code - 修复 GeoTiffParser CRS 提取                       │
│  2. fix-image-projection-mismatch - 前端 TileWMS 添加 projection          │
│  3. fix-image-crs-return-value - 后端返回正确的 CRS                        │
│  4. fix-image-extent-transform - 前端 extent 转换逻辑                     │
│  5. fix-image-extent-transform-final - 前端增加容错判断                   │
│                                                                             │
│  问题仍存在:                                                                │
│  - 后端 CrsTransformUtil 可能转换失败（CRS 格式问题）                     │
│  - 前端条件判断逻辑过于复杂，可能在某些边界情况失败                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 最终修复方案

**核心思路：后端保证 extent 一定是 EPSG:4326，前端无需任何转换**

### 1. 后端强制转换

- 确保 `CrsTransformUtil.transformExtentToWgs84()` 成功执行
- 如果 GeoTools 转换失败，使用简化的数学换算作为备用（EPSG:3857 → EPSG:4326）
- 返回的 extent 一定是经纬度坐标

### 2. 前端简化

- 移除所有 CRS 判断逻辑
- 移除 transformExtent 调用
- 直接使用后端返回的 extent 调用 fit()

## 影响范围

- `backend/.../ImageServiceImpl.java` - 增强 extent 转换逻辑
- `frontend/.../MapContainer.vue` - 简化 extent 使用逻辑

## 风险评估

- 低风险：简化逻辑，移除复杂的条件判断
