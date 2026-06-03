## 1. DTO 层

- [x] 1.1 创建 `ImageDownloadUrlResponse` DTO（含 downloadUrl、fileName、fileSize、expiresIn 字段）

## 2. Service 层

- [x] 2.1 `ImageService` 接口新增 `getDownloadUrl(Long id)` 方法签名
- [x] 2.2 `ImageServiceImpl` 实现 `getDownloadUrl`：查询 dataset + raster_metadata，调用 MinIO 预签名 URL

## 3. Controller 层

- [x] 3.1 `ImageController` 新增 `GET /api/v1/images/{id}/download-url` 端点

## 4. 编译验证

- [x] 4.1 执行 `mvn compile` 检查编译是否通过
