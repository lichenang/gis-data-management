## Why

GWC 种子任务在生成低级别（低 zoom 级别 0-11）瓦片时，请求全球范围的 WMS 数据与影像局部范围冲突，导致 "must intersect with the image's bounds" 错误。即使使用影像实际范围，低级别瓦片的全球范围请求仍然会失败。将最小 zoom 级别从 0 改为 12，跳过必定失败的全球范围低级别瓦片，这些瓦片在实际使用中几乎不会被请求。

## What Changes

1. 修改 GeoServerProperties 配置中的 tilingMinZoom 从 0 改为 12

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/resources/application.yml` — 修改 tiling.min-zoom 配置

## Non-goals

- 不修改切片任务的其他参数（最大 zoom、线程数等）
- 不修改数据库表结构
- 不修改 GeoServer 配置
