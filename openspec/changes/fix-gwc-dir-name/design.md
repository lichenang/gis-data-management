## Context

`TilePackageServiceImpl` 在第 82-83 行构造 GWC 缓存目录路径：

```java
String gwcDirPath = dataDir.replace('\\', '/')
        + "/gwc/" + workspace + "_" + layerName;
```

其中 `workspace = "gisplatform"`（来自 `GeoServerProperties`），`layerName = "raster_" + datasetId`。组合后应为 `gisplatform_raster_38`。

GWC 的实际磁盘目录命名约定为 `{workspace}_{layerName}`（GWC 内部将 `:` 替换为 `_`）。若 GWC 配置中有其他路径前缀或非默认配置，实际目录名可能与预期不符。当前实现假设 GWC 使用默认布局且目录名为 `gisplatform_raster_38`，但未验证该路径确实存在。

## Goals / Non-Goals

**Goals:**
- 验证 GWC 目录路径构造逻辑是否与实际 GWC 磁盘布局一致
- 如有不一致，修正路径拼接逻辑

**Non-Goals:**
- 不修改 GWC 配置或 seed 逻辑
- 不添加 GWC REST API 调用来动态获取目录名

## Decisions

### 决策 1：确认当前路径拼接是否正确

**方案**：检查当前代码 `workspace + "_" + layerName` 是否与实际 GWC 目录名一致。若 GWC 使用不同的命名约定（如 `workspace:layerName` 的其他变体），相应调整。

**当前状态**：代码已使用 `workspace + "_" + layerName` 格式。如实际目录名为不同格式，需改为对应格式。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| GWC 磁盘目录命名与代码假设不一致 | 下载失败，返回"目录不存在" | 确认实际目录命名后修正 |
| 不同 GeoServer 版本/配置有不同目录名 | 迁移环境可能失效 | 保持与 GeoServerCacheService.seedLayer() 中使用的命名一致 |
