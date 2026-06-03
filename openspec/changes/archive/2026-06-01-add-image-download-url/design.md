## Context

影像上传后存入 MinIO（路径 `images/{uuid}.tif`），但当前原始文件下载需经过后端代理流（`ExportController.exportGeoTiff` → `minioClient.getObject` → 后端内存 → response）。大文件场景会占用后端内存和带宽。MinIO Java SDK 8.6.x 提供 `getPresignedObjectUrl()` 方法，可生成临时直链。

## Goals / Non-Goals

**Goals:**
- 新增 `GET /api/v1/images/{id}/download-url` 返回预签名 URL
- 预签名 URL 有效期 5 分钟
- 返回信息包含：URL、原始文件名、文件大小、过期秒数

**Non-Goals:**
- 不修改现有 `exportGeoTiff()`（保留为降级方案）
- 不涉及瓦片包下载或元数据下载

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| MinIO API | `getPresignedObjectUrl(GetPresignedObjectUrlArgs)` | SDK 内置，参数 `METHOD.GET` + `bucket` + `object` + `expiry` |
| 过期时间 | `Duration.ofMinutes(5)` | 平衡安全性和用户体验，足够浏览器完成下载 |
| 数据来源 | 优先 `raster_metadata.minioBucket` + `raster_metadata.minioKey` | 该表明确记录了桶名和对象路径，Dataset.minioKey 作为 fallback |
| 文件名来源 | `raster_metadata.fileName` | 保留原始上传文件名作为下载文件名 |
| 返回格式 | 新增 `ImageDownloadUrlResponse` DTO | 返回结构清晰，不污染现有实体 |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 预签名 URL 泄露 | 有效期仅 5 分钟；后端校验用户是否有该数据集权限 |
| MinIO 地址暴露 | 前端通过预签名 URL 直连 MinIO 是设计意图；如安全要求高可保留代理下载路径 |
