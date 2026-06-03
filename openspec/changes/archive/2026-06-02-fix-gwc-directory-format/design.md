## Context

当前 `TilePackageServiceImpl` 的 `enumerateTileFiles` 方法假设 GWC 瓦片目录结构为标准 OSM 格式 `{z}/{x}/{y}.png`。但实际 GWC 可能使用不同的目录组织方式，最常见的是 `EPSG_4326_{z}/{x}_{y}/{xx}_{yy}.png` 格式：

```
gisplatform_raster_38/
└── EPSG_4326_0/           # layer 0 (zoom level)
    └── 0_0/               # x y combination subdirectory 
        └── 0_0.png        # final tile: {xx}_{yy}.png
```

这意味着代码构建的文件路径 `gwcDir/5/22/14.png` 与实际路径 `gwcDir/EPSG_4326_5/22_14/0_0.png` 完全不匹配。

## Goals / Non-Goals

**Goals:**
- 修改 `enumerateTileFiles` 方法，正确遍历 `EPSG_4326_{z}/{x}_{y}/{xx}_{yy}.png` 格式的瓦片文件
- 保持对标准 `{z}/{x}/{y}.png` 格式的兼容性（自动检测）
- 确保瓦片坐标范围计算与实际格式匹配

**Non-Goals:**
- 不修改 GWC 配置
- 不处理其他非标准格式（如 EPSG:3857 特定格式）

## Decisions

### Decision 1: 自适应目录格式检测

**方案**: 在枚举瓦片前，先检测 GWC 目录的第一层子目录结构：

1. 检查是否存在 `EPSG_4326_*` 或 `EPSG_3857_*` 格式的子目录
2. 如果存在，使用分层格式枚举
3. 如果不存在，使用标准 `{z}/{x}/{y}.png` 格式

**理由**: 保持向后兼容，同时支持新的分层存储格式。

### Decision 2: 分层目录遍历策略

**方案**: 对于 EPSG 格式目录，遍历路径为 `{gwcDir}/EPSG_4326_{z}/{x}_{y}/{xx}_{yy}.png`

**理由**: 这是 GWC 的标准分层存储模式，每层存储 256x256 瓦片。

### Decision 3: 瓦片文件命名转换

**方案**: 标准格式为 `{xx}_{yy}.png`，其中 xx, yy 是 0-255 范围内的瓦片行号。

**理由**: GWC 使用四叉树或类似方式组织，同一目 zoom 下有子目录。

## Risks / Trade-offs

- **[低] 不同 GWC 版本目录结构差异** — 通过自动检测适应不同格式
- **[低] 性能开销** — 需要先检测目录结构，但这是启动时的一次性操作

## Migration Plan

1. 部署修复后的 `TilePackageServiceImpl.java`
2. 测试切片包下载功能
3. 验证不同格式的 GWC 缓存都能正确打包

无数据库迁移需求。

## Open Questions

- 是否需要支持更多 EPSG 代数（如 EPSG:3857）？
- 如果同时存在多种格式，如何处理优先级？
