## Why

GeoServer GWC REST API 调用多次尝试修复均失败：
1. form-urlencoded 格式被拒绝（400 Bad Request）
2. JSON 格式也不被正确处理
3. GWC 缓存目录创建失败（权限或配置问题）

GeoServer 有一个内置机制：当接收到 WMS GetMap 请求时，如果 GWC 缓存已配置，会自动在后台生成对应级别的切片。这是一个更可靠的方式。

## What Changes

1. **修改 TileSeedService.triggerSeed() 方法**
   - 移除对 GeoServerCacheService.seedLayer() 的调用
   - 改为使用 WMS GetMap 请求触发自动切片

2. **新增 WMS 触发切片逻辑**
   - 使用 RestTemplate 或现有 HTTP 客户端发送 WMS GetMap 请求
   - 针对多个 zoom 级别（0-18）发送代表性瓦片请求
   - 每个 zoom 级别发送 1-2 个代表性瓦片即可触发该级别缓存
   - WMS 参数：LAYERS=gisplatform:raster_{id}, FORMAT=image/png, WIDTH=256, HEIGHT=256, SRS=EPSG:3857

3. **保持异步执行**
   - 继续使用 @Async("tilingExecutor") 在后台执行
   - 不阻塞发布流程

## Impact

- `backend/.../service/tiling/TileSeedService.java` — 修改 triggerSeed 方法
- `backend/.../service/geoserver/GeoServerCacheService.java` — 可选：保留或移除 seedLayer 方法

## Non-goals

- 不修改 GeoServer 配置
- 不修改已有的 WMS/WMTS 服务接口

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`
