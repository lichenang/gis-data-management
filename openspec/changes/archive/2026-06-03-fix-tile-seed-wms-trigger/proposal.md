## Why

之前尝试的 GWC REST API 调用方案（JSON 格式）仍然存在问题，返回 400 Bad Request。GeoWebCache REST API 在 GeoServer 2.28.3 中不够稳定。

改用 WMS GetMap 请求触发切片缓存生成是更可靠的方案，因为：
1. WMS 是 GeoServer 最成熟、最稳定的接口
2. GeoServer 会在处理 WMS 请求时自动在后台生成 GWC 切片缓存
3. 无需依赖 GWC REST API 的特定格式

## What Changes

1. **移除 GWC REST API 调用**
   - 移除 `triggerGwcSeedTask()` 方法中的 GWC REST API 调用代码
   - 移除 JSON 请求体构建逻辑

2. **新增 WMS GetMap 请求触发方式**
   - 新增 `triggerWmsSeedRequests()` 方法发送 WMS GetMap 请求
   - 为每个 zoom 级别（0-18）发送代表性瓦片请求
   - 使用影像实际 EPSG:4326 范围作为 BBOX

3. **坐标系转换**
   - 使用 CrsTransformUtil 将 EPSG:3857 范围转换为 EPSG:4326
   - 确保 WMS 请求使用正确的 CRS

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无 - 仅为 bugfix，不改变功能规格）

## Impact

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java` — 重新实现切片触发逻辑

## Non-goals

- 不修改切片任务的业务逻辑（zoom 级别范围、线程数等保持不变）
- 不修改数据库表结构
- 不修改 GeoServer 配置
- 不实现实时进度监控（依赖现有逻辑）

## Affected Files

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`
