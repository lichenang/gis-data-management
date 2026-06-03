# fix-tilewms-crs-param

## 问题描述

影像图层在 EPSG:4326 坐标系下无法显示。

GeoServer 已正确配置 EPSG:4326 网格集，问题出在前端 TileWMS 图层的请求参数配置。

## 原因分析

1. TileWMS 的 `params` 中未显式指定 `CRS: 'EPSG:4326'`
2. 当 `TILED: true` 时，GeoServer 不使用 `CRS` 参数，导致请求的坐标系不正确
3. 缺少正确的 `projection: 'EPSG:4326'` 配置

## 预期结果

修复后，影像图层在 EPSG:4326 坐标系下能够正常显示。
