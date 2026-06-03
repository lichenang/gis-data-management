## Why

之前尝试的 WMS 请求方案存在根本性问题：
1. WMS 请求的 BBOX 硬编码或计算复杂，容易出现坐标范围错误
2. GeoServer 内部处理 WMS 时仍调用 GWC，但错误信息不明确
3. WMS 无法精确控制种子任务的参数（zoom 级别、范围等）

GeoWebCache REST API 是触发种子任务的官方方式，可以精确控制：
- 种子任务类型（seed/reseed/truncate）
- zoom 起始和结束级别
- 地理范围（bounds）
- 并发线程数

## What Changes

1. **修改 TileSeedService.triggerSeed() 方法**
   - 移除 WMS 请求逻辑
   - 改为调用 GeoWebCache REST API

2. **直接使用 GeoServerClient 发送 GWC 种子请求**
   - 端点：`POST /gwc/rest/seed/{layerId}`
   - Content-Type：application/x-www-form-urlencoded
   - 参数：name, zoomStart, zoomStop, format, bounds, threadCount, type

3. **使用影像实际范围作为 bounds 参数**
   - 从 dataset.extent 获取 EPSG:3857 范围
   - 传递正确的 bounds 参数给 GWC

## Impact

- `backend/.../service/tiling/TileSeedService.java` — 重新实现种子任务触发逻辑

## Non-goals

- 不修改数据库表结构
- 不修改 GeoServer 配置

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`

