## Why

后端 `GET /api/v1/images/{id}/download-url` 已实现并返回 MinIO 预签名 URL，但前端影像列表页没有对应的下载入口。用户无法从界面直接下载原始 GeoTIFF 文件。

## What Changes

1. `api/image.ts` 新增 `getImageDownloadUrl(id)` 函数
2. `views/images/index.vue` 操作列新增"下载原始影像"按钮，调用 API 获取 URL 后触发浏览器下载

## 非目标

- 不涉及影像详情页的下载入口（仅在列表页操作列添加）
- 不改变后端

## Capabilities

### New Capabilities
- `image-download-button`: 影像管理页面的原始文件下载功能

### Modified Capabilities

无

## Impact

- `frontend/src/api/image.ts`：新增 `getImageDownloadUrl` 函数和 `ImageDownloadUrlResponse` 接口
- `frontend/src/views/images/index.vue`：操作列新增"下载原始影像"按钮和处理函数
