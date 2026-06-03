# Proposal: fix-image-extent-and-zoom

## Summary

修复影像图层加载后地图不自动缩放到数据区域的问题。选中影像图层后，地图应该自动缩放到该影像的实际覆盖范围，而不是停留在初始的北京市中心位置。

## Problem Statement

当前选中影像图层后：

1. **WMS 请求正常发出**：Network 面板可以看到正确的 GetMap 请求，包含正确的 BBOX 坐标
2. **但地图不缩放**：地图视野仍停留在初始位置（北京，中心 [116.4, 39.9]，zoom 10）
3. **影像可能存在但不可见**：因为缩放级别不合适，影像内容可能超出了当前视图范围

**根本原因分析**：

```
┌─────────────────────────────────────────────────────────────────────┐
│                 DATA FLOW (CURRENT BROKEN)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  发布时 (publishImageDataset):                                       │
│  ┌─────────────────┐      ┌─────────────────────────────────┐      │
│  │ raster_metadata │      │ dataset                          │      │
│  │ .transform      │      │ .wmsUrl = "http://.../raster_27" │      │
│  │ = {"minX":...,  │ ──X──│ .extent = (never set!)          │      │
│  │   "maxY":...}   │      └─────────────────────────────────┘      │
│  └─────────────────┘                                                 │
│                                                                     │
│  加载时 (loadImageLayer):                                            │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │ ImageWmsInfo = { wmsUrl, layerName, crs, opacity }  ← 无 extent! ││
│  └─────────────────────────────────────────────────────────────────┘│
│                                │                                     │
│                                ▼                                     │
│                    TileWMS 创建成功，但 map.getView().fit() 从未调用 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Goals

1. **后端修复**：发布影像时从 `raster_metadata.transform` 提取范围信息，写入 `dataset.extent`
2. **前端修复**：加载影像图层时读取 `extent`，调用 `map.getView().fit()` 自动缩放

## Non-Goals

- 不修改 GeoTiffParser（元数据解析逻辑已正常）
- 不修改矢量图层的加载逻辑
- 不支持动态切换坐标系显示

## Success Criteria

- 选中"汇航广场"影像图层后，地图自动缩放到该影像的实际范围
- 影像在视图中正确居中显示
- 缩放过程有平滑过渡动画

## Affected Files

### Backend
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` - publishImageDataset() 方法

### Frontend
- `backend/src/main/java/com/gisplatform/entity/ImageWmsInfo.java` - 添加 extent 字段
- `frontend/src/api/image.ts` - getImageWmsUrl 返回值包含 extent
- `frontend/src/views/map/MapContainer.vue` - 加载影像时调用 fit()
