# fix-gwc-seed-api-path

## Why

GeoServer 日志显示 GWC 种子状态查询 API 返回 404。根因分析：

当前代码中的路径：
```
/gwc/rest/layers/{layerId}/seed.json
```

GeoServer GWC REST API 正确的路径格式为：
```
/gwc/rest/seed/{layerId}.json
```

路径结构完全不同：`seed/{layerId}` 而非 `layers/{layerId}/seed`

## What Changes

修改 `GeoServerCacheService.java`：
1. `seedLayer()` 方法中的 POST 路径
2. `getSeedStatus()` 方法中的 GET 路径

## Capabilities

### Fixed Capabilities
- GeoWebCache 切片任务触发功能
- GeoWebCache 切片状态查询功能

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`
