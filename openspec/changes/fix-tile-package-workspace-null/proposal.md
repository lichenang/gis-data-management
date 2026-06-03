## Why

切片包下载接口始终返回"该影像尚未生成切片缓存"错误，即使 GWC 缓存目录 `gisplatform_raster_38` 及其瓦片文件已存在。根因是使用 `dataset.getWorkspace()` 构造 GWC 目录路径时，dataset 表中该字段为 null，导致拼接出的路径不正确（如 `null_raster_38`），从而找不到实际缓存文件。

同时，异常处理阶段因 response 已提交（Content-Type 已设为 `application/zip`），GlobalExceptionHandler 无法正常返回 JSON 错误信息。

## What Changes

1. **修复 GWC 目录路径构造** — `TilePackageServiceImpl` 中，在 `dataset.getWorkspace()` 返回 null 时，回退使用 `geoServerProperties.getWorkspace()` 的默认值（`gisplatform`）
2. **修复 GlobalExceptionHandler response 已提交异常** — 在回写 JSON 错误前检测 response 是否已提交，若已提交则跳过写入

## Capabilities

### New Capabilities

- 无（本次变更为缺陷修复，不引入新能力）

### Modified Capabilities

- 无（本次变更不修改已有规范的接口/能力行为）

## Impact

- `backend/.../service/impl/TilePackageServiceImpl.java` — GWC 目录名构造逻辑
- `backend/.../common/GlobalExceptionHandler.java` — response 已提交保护
- 不涉及 API 签名变更、不涉及数据库结构变更、不涉及前端代码变更

## Non-goals

- 不修改瓦片坐标计算逻辑（tileY 公式保持 Web Mercator）
- 不修改 GWC 种子任务触发逻辑
- 不修改数据集元数据表结构或数据
- 不涉及 UI/UX 变化
