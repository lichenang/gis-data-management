## Context

GeoServer 的 coverage store 不仅在 REST API 层面管理，还会在文件系统中创建数据目录（如 `<data_dir>/data/<workspace>/<storename>/`）。之前的修复（删除 coverage store + 等待 2 秒）只清理了 REST API 层面的配置，但文件目录中的数据文件可能仍被 GeoServer 进程持有文件句柄，导致重新创建 store 时失败。

## Goals / Non-Goals

**Goals:**
- 在删除 coverage store 后，清理 GeoServer data_dir 中的对应数据目录
- 清理完成后等待 2 秒确保文件释放

**Non-Goals:**
- 不修改其他发布流程逻辑

## Decisions

### Decision 1: 清理数据目录而非只删除 coverage store

**选择**: 同时清理 GeoServer data_dir 中的数据子目录

**理由**:
- GeoServer 可能在文件系统层面持有文件句柄
- 清理数据目录可以确保彻底释放资源

### Decision 2: 确定数据目录路径

**选择**: `<data_dir>/data/<workspace>/raster_<id>`

**理由**:
- 这是 GeoServer 默认的 ImageMosaic 数据存储路径
- workspace 是 gisplatform，store 名称是 raster_<id>

### Decision 3: 处理目录不存在的情况

**选择**: 使用 FileUtils.deleteDirectory()，捕获异常后继续

**理由**:
- 首次发布时目录不存在是正常情况
- 删除失败不影响主流程

## Implementation Steps

1. 获取 GeoServer data_dir 路径（从 GeoServerProperties.dataDir）
2. 构建数据目录路径：`<dataDir>/data/<workspace>/raster_<id>`
3. 删除该目录（捕获异常）
4. 等待 2 秒
5. 继续创建 coverage store

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| data_dir 未配置 | 未配置 data_dir 时跳过文件清理 | 先检查 dataDir 是否配置 |
| 误删其他数据 | 确保只删除目标数据集的目录 | 使用精确的目录路径 |
