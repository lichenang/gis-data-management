## Why

之前的 GWC 种子任务调用失败，原因之一是网格集名称不匹配。GWC 中注册的米制网格集名称为 "EPSG:900913"，而代码中使用了 "EPSG:3857"。两者是同一坐标系（Web Mercator）的不同名称，需要统一为 GWC 实际使用的名称以确保切片任务能正确触发。

## What Changes

1. 修改 TileSeedService.triggerGwcSeedTask() 方法中的 gridSetId 从 "EPSG:3857" 改为 "EPSG:900913"

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java` — 修改 triggerGwcSeedTask() 方法中的 gridSetId

## Non-goals

- 不修改切片任务的其他逻辑（zoom 级别、线程数、范围等保持不变）
- 不修改数据库表结构
- 不修改 GeoServer 配置
