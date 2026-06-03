## Why

切片包下载接口始终返回 0 个瓦片文件，根因是代码假设 GWC 瓦片目录结构为 `{z}/{x}/{y}.png`，但实际 GWC 使用的是 `EPSG_4326_{z}/{x}_{y}/{xx}_{yy}.png` 分层存储格式。这导致代码计算的瓦片文件路径与磁盘上实际存在的文件路径完全不匹配。

## What Changes

1. **修改 enumerateTileFiles 方法** — 适配 `EPSG_4326_{z}/{x}_{y}/{xx}_{yy}.png` 目录结构，正确遍历 GWC 瓦片文件
2. **修改 getTileRange 方法** — 确保计算的瓦片坐标范围与实际 GWC 存储格式匹配
3. **保持兼容性** — 增加对标准 `{z}/{x}/{y}.png` 格式的自动检测和回退支持

## Capabilities

### New Capabilities
（本次变更为缺陷修复，不引入新能力）

### Modified Capabilities
（本次变更不修改已有规范的接口/能力行为）

## Impact

- `backend/.../service/impl/TilePackageServiceImpl.java` — 修改 enumerateTileFiles 和 getTileRange 方法
- 不涉及 API 签名变更、不涉及数据库结构变更、不涉及前端代码变更

## Non-goals

- 不修改 GWC 配置或 GeoServer 设置
- 不修改瓦片种子任务触发逻辑
- 不涉及 UI/UX 变化

## Affected Files

- `backend/src/main/java/com/gisplatform/service/impl/TilePackageServiceImpl.java`
