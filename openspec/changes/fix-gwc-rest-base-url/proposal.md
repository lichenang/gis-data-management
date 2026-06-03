# fix-gwc-rest-base-url

## Why

GeoServer 日志显示 GWC REST API 请求返回 404。根因分析：

当前代码中 GWC API 路径为：
```
/rest/gwc/layers/{layerId}/seed
```

实际正确的 GeoServer GWC REST API 路径为：
```
/gwc/rest/layers/{layerId}/seed
```

即 `gwc` 和 `rest` 的顺序相反。

## What Changes

修改 `GeoServerCacheService.java` 中所有 GWC REST API 路径：
- `/rest/gwc/` → `/gwc/rest/`

影响范围：
- Line 50: `seedLayer()` 方法中的 POST 路径
- Line 59: `getSeedStatus()` 方法中的 GET 路径

## Capabilities

### Fixed Capabilities
- GeoWebCache 切片任务触发功能
- GeoWebCache 切片状态查询功能

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`

## Non-goals

- 不修改 GeoServerClient 的基础 URL 拼接逻辑
- 不修改 WMTS 相关路径（`/gwc/service/wmts` 是正确的）
