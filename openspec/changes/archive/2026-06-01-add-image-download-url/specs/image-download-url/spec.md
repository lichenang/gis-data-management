## 模块划分

此变更涉及 Service 层、Controller 层和 DTO 层。

| 模块 | 说明 |
|------|------|
| DTO 层 | `ImageDownloadUrlResponse` 新类 |
| Service 层 | `ImageService.getDownloadUrl()` 方法新增与实现 |
| Controller 层 | `ImageController.getDownloadUrl()` 新端点 |

## 数据流设计

```
GET /api/v1/images/{id}/download-url
  │
  ├── dataset = getById(id)
  │     └── null or type!=raster → 404
  │
  ├── raster_metadata = selectOne(datasetId)
  │     └── null → 400 "影像元数据不存在"
  │
  ├── bucket = rasterMetadata.minioBucket ?? rasterBucket
  ├── key    = rasterMetadata.minioKey ?? dataset.minioKey
  │     └── null → 400 "影像文件不存在"
  │
  ├── presignedUrl = minioClient.getPresignedObjectUrl(
  │     GetPresignedObjectUrlArgs.builder()
  │       .method(GET)
  │       .bucket(bucket)
  │       .object(key)
  │       .expiry(Duration.ofMinutes(5))
  │       .build())
  │
  └── return ImageDownloadUrlResponse(url, fileName, fileSize, expiresIn=300)
```

## 接口列表

| 接口 | 方法 | 参数 | 返回 |
|------|------|------|------|
| `/api/v1/images/{id}/download-url` | GET | `id` (path) | `R<ImageDownloadUrlResponse>` |

## ADDED Requirements

### Requirement: 获取影像原始文件下载 URL
系统 SHALL 提供 API 返回 MinIO 预签名下载 URL，有效期 5 分钟。

#### Scenario: 成功获取下载 URL
- **WHEN** 请求 `GET /api/v1/images/{id}/download-url` 且数据集存在、类型为 raster、raster_metadata 完整
- **THEN** 返回 `{"code":200, "data":{"downloadUrl":"http://...", "fileName":"原始名称.tif", "fileSize":256000000, "expiresIn":300}}`

#### Scenario: 数据集不存在
- **WHEN** 请求 `GET /api/v1/images/{id}/download-url` 且数据集不存在或已删除
- **THEN** 返回 `{"code":404, "message":"影像数据集不存在"}`

#### Scenario: 数据集非影像类型
- **WHEN** 请求 `GET /api/v1/images/{id}/download-url` 且数据集类型不是 raster
- **THEN** 返回 `{"code":400, "message":"仅支持影像数据集"}`

#### Scenario: 影像元数据不存在
- **WHEN** 请求 `GET /api/v1/images/{id}/download-url` 且 raster_metadata 记录不存在
- **THEN** 返回 `{"code":400, "message":"影像元数据不存在"}`
