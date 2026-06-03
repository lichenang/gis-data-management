## Why

影像数据集上传后存储在 MinIO，但当前下载原始 GeoTIFF 只能通过 `ExportController.exportGeoTiff()` 流式代理下载（走后端内存中转）。大文件（100MB+）场景后端需二次流转，带宽和内存开销大。应通过 MinIO 预签名 URL 让前端直连下载，减轻后端压力。

## What Changes

1. `ImageService` 接口新增 `getDownloadUrl(Long id)` 方法
2. `ImageServiceImpl` 实现该方法：查询 `raster_metadata` 获取 MinIO 路径，调用 `minioClient.getPresignedObjectUrl()` 生成 5 分钟有效期的预签名 URL
3. `ImageController` 新增 `GET /api/v1/images/{id}/download-url` 端点
4. 新增 DTO `ImageDownloadUrlResponse` 承载返回结构

## 非目标

- 不涉及切片包下载、元数据下载
- 不涉及前端代码

## Capabilities

### New Capabilities
- `image-download-url`: 影像原始文件预签名 URL 下载

### Modified Capabilities

无

## Impact

- `backend/src/main/java/com/gisplatform/service/ImageService.java`：新增 `getDownloadUrl` 方法签名
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`：实现预签名 URL 生成逻辑
- `backend/src/main/java/com/gisplatform/controller/ImageController.java`：新增下载 URL 端点
- `backend/src/main/java/com/gisplatform/dto/ImageDownloadUrlResponse.java`：新增 DTO
