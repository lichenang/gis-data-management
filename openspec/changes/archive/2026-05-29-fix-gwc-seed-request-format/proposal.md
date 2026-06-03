# fix-gwc-seed-request-format

## Why

GeoServer 2.28.3 的 GWC SeedController 在处理种子触发请求时出现解析错误：

```
Chunk [<?xml] is not a valid entry
```

当前代码使用 XML 格式发送请求体：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<seed>
  <name>{layerId}</name>
  <zoomStart>{minZoom}</zoomStart>
  ...
</seed>
Content-Type: application/xml
```

但 GeoServer 2.28.3 的 GWC SeedController 期望的是 `application/x-www-form-urlencoded` 格式。

## What Changes

修改 `GeoServerCacheService.java` 中 `seedLayer()` 方法：
1. 将请求头 Content-Type 改为 `application/x-www-form-urlencoded`
2. 将请求体从 XML 格式改为 URL 编码参数格式

## Capabilities

### Fixed Capabilities
- GeoWebCache 切片任务触发功能

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`
