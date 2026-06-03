# Proposal: fix-image-crs-mismatch

## 问题描述

影像图层加载后：
1. 地图画面空白（不显示影像）
2. 地图跳转到了错误位置（而非影像实际位置）

## 根因分析

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        核心问题                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  后端 ImageServiceImpl.getImageWmsInfo() (line 408-410):                    │
│                                                                             │
│    info.setExtent(transformedExtent);  // extent 已转换为 EPSG:4326         │
│    info.setCrs("EPSG:3857");           // ← BUG! CRS 仍是 "EPSG:3857"       │
│                                                                             │
│  extent 是经纬度 [116.2, 39.6, 116.6, 40.0] (EPSG:4326)                     │
│  但 crs 字段错误地返回 "EPSG:3857" (米制坐标系)                              │
│                                                                             │
│  前端 MapContainer.vue (line 255-263):                                      │
│  ┌──────────────────────────────────────┐                                   │
│  │ const wmsSource = new TileWMS({      │                                   │
│  │   url: imageInfo.wmsUrl,             │                                   │
│  │   params: {                          │                                   │
│  │     'LAYERS': imageInfo.layerName,   │                                   │
│  │     'TILED': true                    │                                   │
│  │   },                                 │                                   │
│  │   serverType: 'geoserver',           │                                   │
│  │   transition: 0                      │                                   │
│  │   // ← 缺少 projection 参数!         │                                   │
│  │ })                                   │                                   │
│  └──────────────────────────────────────┘                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Bug #1: 后端 CRS 设置错误

`ImageServiceImpl.java:408-410`:
- extent 被 CrsTransformUtil 转换为 EPSG:4326（经纬度）
- 但 info.setCrs("EPSG:3857") 错误地设为源 CRS

### Bug #2: 前端 TileWMS 未指定 projection

`MapContainer.vue:255-263`:
- TileWMS 没有 projection 参数
- OpenLayers 使用 view 的 projection (EPSG:4326) 发送 WMS 请求
- 但 GeoServer 影像数据实际存储在 EPSG:3857
- BBOX 范围不匹配导致请求返回空白

## 修复方案

### 修复 1: 后端 ImageServiceImpl.getImageWmsInfo()

```java
// ImageServiceImpl.java line 408-410
if (transformedExtent != null) {
    info.setExtent(transformedExtent);  // EPSG:4326 经纬度
    info.setCrs("EPSG:4326");           // ← 改为 EPSG:4326
} else {
    double[] fallbackExtent = transformExtentSimple(extent);
    info.setExtent(fallbackExtent);
    info.setCrs("EPSG:4326");           // ← 同样改为 EPSG:4326
}
```

### 修复 2: 前端 MapContainer.vue TileWMS

```typescript
// MapContainer.vue line 255-263
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

## 影响范围

| 文件 | 修改内容 | 风险 |
|------|----------|------|
| `backend/.../ImageServiceImpl.java` | setCrs("EPSG:3857") → setCrs("EPSG:4326") | 低 |
| `frontend/src/views/map/MapContainer.vue` | TileWMS 添加 projection 参数 | 低 |

## 验证方法

1. 启动后端和前端
2. 上传并发布 GeoTIFF 影像
3. 在地图上添加影像图层
4. 验证：
   - 地图自动缩放到正确区域（而非海里）
   - 影像正确显示
