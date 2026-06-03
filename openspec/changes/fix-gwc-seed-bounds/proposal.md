## Why

GWC 种子任务启动后，在生成低级别（低分辨率）瓦片时请求全球范围的 WMS 数据，与影像实际覆盖区域冲突，导致 "must intersect with the image's bounds" 错误。需要使用影像的实际范围或 GWC 图层配置的范围来解决此问题。

## What Changes

1. 修改 TileSeedService.triggerGwcSeedTask() 方法，在 XML 请求体中添加 `useCurrentBounds` 参数设为 `true`，让 GWC 自动使用图层的实际范围

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java` — 修改 triggerGwcSeedTask() 方法中的 XML 请求体

## Non-goals

- 不修改切片任务的 zoom 级别、线程数等其他参数
- 不修改数据库表结构
- 不修改 GeoServer 配置
