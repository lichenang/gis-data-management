## Why

GWC 种子任务启动后，在生成低级别（低分辨率）瓦片时请求全球范围的 WMS 数据，与影像实际覆盖区域冲突，导致 "must intersect with the image's bounds" 错误。之前的修复（useCurrentBounds=true）在某些版本 GWC 上不生效，需要直接使用影像的实际 EPSG:900913 范围作为 bounds。

## What Changes

1. 修改 TileSeedService.triggerGwcSeedTask() 方法，确保使用影像的实际 EPSG:900913 范围
2. 从 dataset.getExtent() 获取影像实际范围（如无则使用全球范围）

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java` — 修改 triggerGwcSeedTask() 方法

## Non-goals

- 不修改切片任务的 zoom 级别、线程数等其他参数
- 不修改数据库表结构
- 不修改 GeoServer 配置
