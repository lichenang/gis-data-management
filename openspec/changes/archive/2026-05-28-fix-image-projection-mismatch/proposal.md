# Proposal: fix-image-projection-mismatch

## 问题描述

影像数据为 EPSG:3857，地图视图使用 EPSG:4326，导致：
1. WMS 请求使用错误的坐标系
2. `fit(extent)` 使用错误的坐标值

错误现象：地图加载后一片空白。

## 根因分析

根据诊断报告 `openspec/specs/debug-image-coordinate-mismatch.md`：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        问题链路                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  前端 MapContainer.vue:                                                │
│                                                                         │
│  1. TileWMS 没有 projection 参数                                        │
│     → OpenLayers 默认用 view projection (EPSG:4326) 发送 WMS 请求     │
│     → 但 GeoServer 影像数据是 EPSG:3857 → 范围不匹配                   │
│                                                                         │
│  2. fit(extent) 使用 imageInfo.extent                                  │
│     → 若后端转换失败，extent 仍是 EPSG:3857 坐标                       │
│     → 直接 fit 到 EPSG:4326 视图 → 范围完全错误                        │
│                                                                         │
│  后端 ImageServiceImpl.getImageWmsInfo():                              │
│  - 调用 CrsTransformUtil.transformExtentToWgs84()                     │
│  - 依赖 dataset.getSrs() 返回正确的 EPSG 代码                          │
│  - 之前的 fix-srs-epsg-code 修复是否生效需验证                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 修复方案

### 1. 前端 TileWMS 添加 projection 参数

**文件**: `frontend/src/views/map/MapContainer.vue`

```javascript
const wmsSource = new TileWMS({
  url: imageInfo.wmsUrl,
  params: {
    'LAYERS': imageInfo.layerName,
    'TILED': true
  },
  projection: imageInfo.crs || 'EPSG:4326',  // ← 添加 projection
  serverType: 'geoserver',
  transition: 0
})
```

### 2. 前端加载影像后正确处理 extent 转换

**当前代码问题** (`MapContainer.vue:278-285`):
```javascript
if (imageInfo.extent && imageInfo.extent.length === 4) {
  const extent: [number, number, number, number] = imageInfo.extent
  map.value.getView().fit(extent, { ... })
}
```

**问题**: `imageInfo.extent` 可能已是 EPSG:4326（后端已转换），也可能仍是 EPSG:3857（后端转换失败）。

**方案**: 使用 `imageInfo.crs` 判断：
- 如果 `crs === 'EPSG:4326'` 或 `!crs`，extent 已是 4326，直接使用
- 如果 `crs === 'EPSG:3857'`，需要使用 OpenLayers 的 transform 方法转换 extent

### 3. 后端确保 extent 转换正确

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

确保在 `getImageWmsInfo()` 方法中：
1. 正确调用 `CrsTransformUtil.transformExtentToWgs84()`
2. 添加详细日志确认转换成功/失败
3. 返回正确的 `crs` 字段（应为 "EPSG:4326"）

## 影响范围

- 前端: `MapContainer.vue` - loadImageLayer() 函数
- 后端: `ImageServiceImpl.java` - getImageWmsInfo() 方法（增强日志）

## 风险评估

- 低风险：前端仅添加参数和条件判断，后端仅添加日志
