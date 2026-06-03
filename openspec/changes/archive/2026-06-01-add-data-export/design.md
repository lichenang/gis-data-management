## Context

当前系统支持矢量数据集（PostGIS）和影像数据集（MinIO）的管理与发布，但缺少数据导出到通用格式的能力。用户无法将数据从系统中取出供外部GIS工具使用。

系统中已存在一个 `GET /{id}/geojson` 端点通过 `ST_AsGeoJSON` 查询，GeoTools 32.0 依赖已引入（含 gt-jdbc-postgis、gt-shapefile），MinIO SDK 已集成。

## Goals / Non-Goals

**Goals:**
- 矢量数据集（PostGIS）支持导出 GeoJSON、Shapefile（ZIP）、KML
- 影像数据集（MinIO）下载原始 GeoTIFF
- 前端操作列添加"导出"下拉菜单
- 文件通过 `Content-Disposition: attachment` 触发浏览器下载

**Non-Goals:**
- 不支持矢量→GeoTIFF 或影像→矢量格式转换
- 不添加导出进度条
- 不涉及权限校验（复用现有认证机制）
- 不涉及批量导出

## Decisions

| 决策 | 方案 | 备选方案 | 原因 |
|---|---|---|---|
| GeoJSON 导出 | `ST_AsGeoJSON()` + JdbcTemplate | GeoTools FeatureWriter | 已有现成代码，无额外复杂度 |
| KML 导出 | `ST_AsKML()` + 手动 KML 组装 | GeoTools KML 编码器 | 避免引入新依赖，SQL 直接取 geometry KML |
| Shapefile 导出 | GeoTools PostGIS DataStore → ShapefileDataStore → ZIP | -- | GeoTools 已引入，支持完整属性写入 |
| GeoTIFF 导出 | MinIO 流式直传 response | 下载到临时文件再返回 | 减少磁盘 I/O，支持大文件 |
| 端点位置 | 新建 ExportController | 追加到 DatasetController | 遵循单一职责，不与业务端点混淆 |
| 前端下载 | `<a>` 标签直连 URL | axios 下载 blob | 后端直接流式，无需前端处理二进制 |

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| Shapefile 字段名被截断为10字符 | GeoTools ShapefileDataStore 自动处理 |
| 大 Shapefile 占用临时磁盘空间 | 使用 `Files.createTempDirectory`，finally 中确保删除 |
| PostGIS 连接通过 GeoTools 复用数据源 | 读取 DataSource 连接参数构造 `PostgisNGDataStoreFactory` 参数 |
| 前端硬编码 baseURL 跨域问题 | `download` 属性触发同源下载，无需额外 CORS 处理 |
