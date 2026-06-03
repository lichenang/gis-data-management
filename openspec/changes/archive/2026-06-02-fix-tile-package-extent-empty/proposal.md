## Why

切片包下载接口始终返回"该影像尚未生成切片缓存"错误，即使 GWC 缓存目录 `gisplatform_raster_38` 及其瓦片文件已存在。根因是 `dataset.extent` 字段为空时，代码回退到全球范围 (-180,-90,180,90)，但实际瓦片只在西安区域生成，导致瓦片坐标计算结果与实际 GWC 存储的瓦片坐标不匹配，从而找不到任何瓦片文件。

## What Changes

1. **增强 extent 获取逻辑** — 在 `TilePackageServiceImpl.packageTiles` 方法中，当 `dataset.getExtent()` 为空时，尝试从 `raster_metadata` 表获取实际影像范围
2. **放宽瓦片查找策略** — 当 extent 仍然为空时，改为遍历 GWC 目录下所有 `.png` 文件，而非依赖地理范围计算匹配
3. **添加调试日志** — 在 `enumerateTileFiles` 方法中输出实际扫描的目录路径、文件数等诊断信息

## Capabilities

### New Capabilities
（本次变更为缺陷修复，不引入新能力）

### Modified Capabilities
（本次变更不修改已有规范的接口/能力行为，仅为实现层修复）

## Impact

- `backend/.../service/impl/TilePackageServiceImpl.java` — extent 获取逻辑、瓦片枚举逻辑
- `backend/.../mapper/RasterMetadataMapper.java`（如需新增）— raster_metadata 表查询
- 不涉及 API 签名变更、不涉及数据库结构变更、不涉及前端代码变更

## Non-goals

- 不修改瓦片坐标计算逻辑（tileY 公式保持 Web Mercator）
- 不修改 GWC 种子任务触发逻辑
- 不修改数据集元数据表结构
- 不涉及 UI/UX 变化

##  Affected Files

- `backend/src/main/java/com/gisplatform/service/impl/TilePackageServiceImpl.java`
