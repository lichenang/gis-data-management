# Proposal: fix-image-extent-transform-final

## 问题描述

影像图层勾选后，地图跳转到海里（西非附近），影像完全不显示。

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        问题根因                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  后端 getImageWmsInfo():                                                    │
│  ├─ 从 dataset.srs 获取原始 CRS                                            │
│  ├─ 使用 CrsTransformUtil 转换 extent 到 EPSG:4326                        │
│  └─ 返回: { extent: ?, crs: sourceCrs }                                    │
│                                                                             │
│  问题: imageInfo.crs 可能是非标准格式（如 "EPSG:WGS 84 / Pseudo-Mercator"）│
│  如果 CRS 转换失败，后端返回原始 extent (EPSG:3857 米制坐标)                │
│                                                                             │
│  前端 loadImageLayer():                                                     │
│  ├─ 条件判断: imageInfo.crs === 'EPSG:3857'                                │
│  ├─ 但实际 crs 可能不是这个值                                              │
│  ├─ 条件不匹配 → 不转换 extent                                            │
│  └─ 直接 fit(EPSG:3857 坐标) → 地图飞海里                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

### 后端修复

保证 `dataset.srs` 是标准格式（"EPSG:3857"），确保 CrsTransformUtil 正确工作。

### 前端修复 - 核心

增加 **extent 数值范围容错判断**：

```javascript
// 判断 extent 是否看起来像 EPSG:4326 (经纬度范围)
const extentLooksLike4326 = 
    extent[0] >= -180 && extent[0] <= 180 &&
    extent[1] >= -90 && extent[1] <= 90 &&
    extent[2] >= -180 && extent[2] <= 180 &&
    extent[3] >= -90 && extent[3] <= 90;

// 判断 view 是否为 EPSG:4326
const isView4326 = viewProjection === 'EPSG:4326';

// 决策
if (isView4326 && !extentLooksLike4326) {
    // view 是 4326 但 extent 是米制坐标 → 需要转换
    transformExtentAndFit(extent, sourceCrs, viewProjection);
} else {
    // extent 已经是 4326 或 view 不是 4326 → 直接使用
    view.fit(extent, {...});
}
```

## 影响范围

- `backend/.../ImageServiceImpl.java` - 确认 crs 返回逻辑
- `frontend/.../MapContainer.vue` - 增强 extent 转换容错

## 风险评估

- 低风险：仅修复判断逻辑，不影响正常场景
