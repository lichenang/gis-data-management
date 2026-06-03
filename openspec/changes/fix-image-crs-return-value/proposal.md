# Proposal: fix-image-crs-return-value

## 问题描述

影像发布到 GeoServer 后，在地图上选择影像时，地图变成一片空白。

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  后端 getImageWmsInfo():                                                    │
│  ├─ 从 GeoTIFF 解析得到 extent (原始坐标系，如 EPSG:3857)                  │
│  ├─ 调用 CrsTransformUtil 转换 extent 到 EPSG:4326 (用于前端 fit 视图)    │
│  └─ info.setCrs("EPSG:4326")  ← 错误！                                     │
│                                                                             │
│  前端 TileWMS:                                                              │
│  ├─ projection: "EPSG:4326"                                                │
│  ├─ WMS 请求使用 EPSG:4326 坐标系                                          │
│  └─ 但 GeoServer 影像数据实际存储在 EPSG:3857 → 查不到数据                │
│                                                                             │
│  结果: 影像图层加载成功但瓦片请求范围错误，显示空白                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

`crs` 字段的语义是：**GeoServer 影像图层使用的实际 CRS**，不是转换后的 extent CRS。

## 修复方案

修改 `ImageServiceImpl.getImageWmsInfo()` 方法中的 CRS 返回逻辑：

```java
// 错误 ❌
info.setCrs("EPSG:4326");

// 正确 ✓
info.setCrs(sourceCrs != null ? sourceCrs : "EPSG:4326");
```

## 影响范围

- 修改: `backend/.../ImageServiceImpl.java` - getImageWmsInfo() 方法

## 风险评估

- 低风险：仅修改返回的 CRS 值，不改变业务逻辑
