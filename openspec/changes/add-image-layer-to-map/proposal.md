# Proposal: add-image-layer-to-map

## Summary

在地图查看页面左侧面板增加"影像图层"分类，加载已发布的影像数据集，使用 OpenLayers 的 TileWMS 或 WMTS 加载 GeoServer 影像服务，与现有矢量图层共存，支持透明度调节。

## Problem Statement

当前地图查看页面仅支持矢量图层（GeoJSON），无法叠加显示影像图层。用户需要查看已发布的影像数据（如卫星影像、航空像片）与矢量数据叠加展示，以进行更直观的地理空间分析。

## Background

参考现有实现：
- 已有地图查看页面 `frontend/src/views/map/index.vue`
- 已有矢量图层加载逻辑 `MapContainer.vue` 使用 VectorSource + VectorLayer
- 已有影像管理 `frontend/src/views/images/index.vue`
- 已有影像 API `frontend/src/api/image.ts`
- OpenLayers 已安装 (ol: ^10.0.0)，支持 TileWMS、WMTS、ImageWMS

## Goals

1. 后端：新增查询已发布影像数据集接口 `GET /api/v1/images/published`
2. 后端：新增获取影像图层 WMS 地址接口 `GET /api/v1/images/{id}/wms-url`
3. 前端：在 LayerPanel 添加"影像图层"分类，与矢量图层分开显示
4. 前端：使用 TileWMS 或 WMTS 加载影像图层
5. 前端：支持影像图层显示/隐藏切换
6. 前端：支持影像图层透明度调节

## Non-Goals

- 不实现影像图层的样式编辑
- 不实现影像图层的属性查询（GetFeatureInfo）
- 不实现多时相影像的时间切换

## Success Criteria

- `GET /api/v1/images/published` 返回已发布影像数据集列表
- `GET /api/v1/images/{id}/wms-url` 返回 GeoServer WMS 访问地址
- LayerPanel 显示两个分类：矢量图层 / 影像图层
- 影像图层使用 TileWMS 加载到地图上
- 可独立切换影像图层的显示/隐藏
- 影像图层支持透明度滑块调节（0-100%）
- 影像图层与矢量图层可叠加显示

## Affected Files

### Backend
- `backend/src/main/java/com/gisplatform/controller/ImageController.java` - 新增接口
- `backend/src/main/java/com/gisplatform/service/ImageService.java` - 新增方法
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` - 实现

### Frontend
- `frontend/src/api/image.ts` - 新增 API 函数
- `frontend/src/views/map/LayerPanel.vue` - 添加影像图层分类
- `frontend/src/views/map/MapContainer.vue` - 添加影像图层加载逻辑
