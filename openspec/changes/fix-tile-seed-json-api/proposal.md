## Why

当前自动切片功能调用 GeoWebCache REST API 返回 400 Bad Request，导致切片任务无法正常触发。根据诊断分析，现有的 form-urlencoded 格式请求不符合 GeoServer 2.28.3 的预期，需要改用 JSON 格式。

## What Changes

1. **修改 TileSeedService.triggerGwcSeedTask() 方法**
   - 将 Content-Type 从 `application/x-www-form-urlencoded` 改为 `application/json`
   - 请求体改用 JSON 格式：`{"seedRequest": {...}}`
   - 显式添加 `gridSetId` 参数，值为 `EPSG:3857`
   - 使用影像的实际 EPSG:3857 范围作为 bounds 参数

2. **添加 gridSetId 参数**
   - 在 JSON 请求体中显式指定 `gridSetId: "EPSG:3857"`
   - 确保 bounds 坐标系与 gridSetId 匹配

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java` — 修改 triggerGwcSeedTask() 方法

## Non-goals

- 不修改数据库表结构
- 不修改 GeoServer 配置
- 不修改切片任务的参数逻辑（zoom 级别、线程数等）

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`
