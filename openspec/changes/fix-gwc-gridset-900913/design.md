## Context

GeoWebCache (随 GeoServer 发行) 使用的网格集名称:
- EPSG:900913 - GWC 默认注册的米制网格集名称（等同于 EPSG:3857）
- EPSG:3857 - OGC 标准定义的 Web Mercator 代码

当前代码中使用的是 "EPSG:3857"，但 GWC REST API 使用的网格集名称是 "EPSG:900913"。

## Goals / Non-Goals

**Goals:**
- 将 TileSeedService 中使用的 gridSetId 从 "EPSG:3857" 改为 "EPSG:900913"

**Non-Goals:**
- 不修改切片任务的其他逻辑

## Decisions

### Decision 1: 使用 EPSG:900913

**选择**: "EPSG:900913"

**理由**:
- GWC 默认网格集使用 EPSG:900913 名称
- 与 GWC REST API 的预期一致

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| GWC 版本差异 | 不同版本的 GWC 可能使用不同网格集名称 | 如果失败，可以改回 EPSG:3857 |
