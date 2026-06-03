# fix-gwc-api-path

## Why

GeoServer 日志显示对 `/rest/gwc/layers/{layer}/seeds.json` 的请求返回 404。问题有两个：

1. **API 路径错误** - GeoServer GWC REST API 使用 `/seed.json`（单数），不是 `/seeds.json`（复数）
2. **JSON 解析逻辑错误** - 代码期望 `{"runs": [...]}` 格式，但实际返回 `{"long": {...}}` 格式

## What Changes

修改 `GeoServerCacheService.java`:

1. 将 `getSeedStatus` 方法中的 URL 从 `/seeds.json` 改为 `/seed.json`
2. 将 JSON 解析逻辑从 `root.get("runs")` 改为 `root.get("long")`

## Capabilities

### Fixed Capabilities
- 能够正确查询 GeoWebCache 切片状态
- 切片进度展示功能恢复正常

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`

## Non-goals

- 不修改 seed（触发切片任务）的 API 路径
- 不修改其他 GeoServer 相关的 API
