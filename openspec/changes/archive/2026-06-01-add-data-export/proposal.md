## Why

当前系统支持矢量数据集（PostGIS）和影像数据集（MinIO）的上传、发布与管理，但缺少数据导出能力。用户无法将已管理的数据集导出为通用格式（GeoJSON、Shapefile、KML、GeoTIFF）供外部使用，限制了数据的流通与共享。

## What Changes

- 新增 `GET /api/v1/datasets/{id}/export?format=xxx` 数据导出端点
- 矢量数据集（PostGIS）支持导出为 GeoJSON、Shapefile（ZIP 压缩包）、KML
- 影像数据集（MinIO）支持导出为原始 GeoTIFF 文件
- 前端数据集管理页面操作列增加"导出"下拉按钮，根据数据集类型按需展示格式选项
- Shapefile 导出使用 GeoTools 进行格式转换打包

## Capabilities

### New Capabilities
- `data-export`: 数据集导出能力，支持矢量/影像数据集的格式转换与文件下载

### Modified Capabilities

（无）

## 非目标

- 不支持矢量数据集导出为 GeoTIFF
- 不支持影像数据集导出为矢量格式
- 不添加导出进度条（大文件直接流式下载）
- 不涉及权限校验（复用现有认证机制）
- 不涉及批量导出（每次仅导出一个数据集）

## Impact

| 文件 | 操作 | 说明 |
|---|---|---|
| `.../controller/ExportController.java` | 新增 | 导出端点 |
| `.../service/DatasetService.java` | 修改 | 新增 export 方法声明 |
| `.../service/impl/DatasetServiceImpl.java` | 修改 | 新增 GeoJSON/KML/Shapefile 导出实现 |
| `.../util/ExportUtil.java` | 新增 | 工具方法（ZIP 打包、文件名清理等） |
| `frontend/src/views/datasets/index.vue` | 修改 | 添加导出下拉按钮 |
