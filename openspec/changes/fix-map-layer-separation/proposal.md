# Proposal: fix-map-layer-separation

## Summary

修复地图页面图层列表中矢量图层和影像图层错误混合显示的问题，以及影像图层 TileWMS 加载失败的问题。

## Problem Statement

当前地图查看页面的图层列表存在两个 Bug：

**Bug 1: 矢量图层列表错误包含影像数据**

`DatasetServiceImpl.listPublishedDatasets()` 方法仅按 `status='published'` 和 `deleted=0` 过滤，缺少 `type` 过滤条件。这导致影像数据集（`type='raster'`）同时出现在"矢量图层"和"影像图层"两个分类中。

**Bug 2: 影像图层 WMS URL 构建错误**

`ImageServiceImpl.getImageWmsInfo()` 方法手动拼接 WMS URL：

```java
info.setWmsUrl(geoServerProperties.getUrl() + "/" + geoServerProperties.getWorkspace() + "/wms");
```

丢失了图层名（`raster_{id}`），导致前端 TileWMS 请求的 URL 错误（如 `http://localhost:8080/geoserver/gisplatform/wms`），无法加载影像。

实际上在发布时 `Dataset` 已存储了正确的 WMS URL（`dataset.setWmsUrl(layerService.getWmsUrl(workspace, layerName))`），应直接使用。

## Goals

1. 修复 `DatasetServiceImpl.listPublishedDatasets()` 添加矢量类型过滤
2. 修复 `ImageServiceImpl.getImageWmsInfo()` 使用 `dataset.getWmsUrl()`

## Non-Goals

- 不修改前端代码
- 不修改图层发布流程

## Success Criteria

- 矢量图层列表只显示 `type='vector'` 的已发布数据集
- 影像图层列表正确加载 GeoServer WMS 影像
- 选中影像图层复选框后，地图上正确显示影像

## Affected Files

- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
