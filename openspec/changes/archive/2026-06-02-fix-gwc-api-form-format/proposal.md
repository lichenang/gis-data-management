## Why

上一轮修复 (fix-gwc-api-json-format) 错误地将 GWC REST API 请求改为 JSON 格式。根据对 GeoServer 2.28.3 源码的分析，GWC SeedController.doPost 实际期望 `application/x-www-form-urlencoded` 格式。需要修正为正确的表单参数格式。

## What Changes

1. **修改 GeoServerCacheService.seedLayer() 方法**
   - 将 Content-Type 从 application/json 改回 application/x-www-form-urlencoded
   - 使用 MultiValueMap 构造正确的表单参数（name, zoomStart, zoomStop, format, threadCount, type 等）
   - 移除错误的 seedRequest JSON 包装
   - 确保请求体格式与 GeoServer 2.28.3 SeedController.doPost 的期望一致

## Impact

- `backend/.../service/geoserver/GeoServerCacheService.java` — 修改 seedLayer 方法

## Non-goals

- 不修改其他 API 端点
- 不修改 GeoServerClient 的通用逻辑

## Affected Files

- `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`
