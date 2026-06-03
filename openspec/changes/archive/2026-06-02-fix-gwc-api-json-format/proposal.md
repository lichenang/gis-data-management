## Why

GeoServerCacheService.seedLayer() 发送的 GWC REST API 请求使用 `application/x-www-form-urlencoded` 格式，但 GeoServer 2.28.3 期望 `application/json` 格式。导致影像发布后自动切片任务从未真正启动。

## What Changes

1. **修改 GeoServerCacheService.seedLayer() 方法**
   - 将 Content-Type 从 `application/x-www-form-urlencoded` 改为 `application/json`
   - 将请求体从 form-urlencoded 字符串改为正确的 JSON 结构（seedRequest、zoomStart、zoomStop、format、bounds、threadCount）
   - 异常处理从 log.warn 改为 log.error + 抛出 RuntimeException

2. **可选：尝试新版 API 端点**
   - 如 `/gwc/rest/seed/...` 失败，尝试 `/rest/gwc/layers/{layer}/seed`

## Impact

- `backend/.../service/geoserver/GeoServerCacheService.java` — 修改 seedLayer 方法

## Non-goals

- 不修改 GeoServerClient 的通用逻辑
- 不修改 TileSeedService 的调用逻辑

## Affected Files

- `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`
