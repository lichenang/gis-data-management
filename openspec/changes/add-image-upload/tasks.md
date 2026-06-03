# Tasks: add-image-upload

## Task 1: 创建 RasterMetadata 实体

**File**: `backend/src/main/java/com/gisplatform/entity/RasterMetadata.java`

- [x] 已完成

## Task 2: 创建 RasterMetadataMapper

**File**: `backend/src/main/java/com/gisplatform/mapper/RasterMetadataMapper.java`

- [x] 已完成

## Task 3: 创建 GeoTIFF 解析工具类

**File**: `backend/src/main/java/com/gisplatform/util/GeoTiffParser.java`

- [x] 已完成

## Task 4: 创建 ImageService

**Files**:
- `backend/src/main/java/com/gisplatform/service/ImageService.java`
- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`
- `backend/src/main/java/com/gisplatform/config/MinioConfig.java`

- [x] 已完成

## Task 5: 创建 ImageController

**File**: `backend/src/main/java/com/gisplatform/controller/ImageController.java`

- [x] 已完成

## Task 6: 添加前端路由

**File**: `frontend/src/router/index.ts`

- [x] 已完成

## Task 7: 创建影像列表页

**File**: `frontend/src/views/images/index.vue`

- [x] 已完成

## Task 8: 实现上传对话框

**File**: `frontend/src/views/images/index.vue` 内嵌对话框

- [x] 已完成（合并到 Task 7）

## Task 9: 添加 API 封装

**File**: `frontend/src/api/image.ts`

- [x] 已完成

## Task 10: 验证

1. 后端：启动应用，用 Swagger 或 Postman 测试 /api/v1/images/upload
2. 前端：访问 /images 页面，验证上传和列表功能

- [ ] 待验证
