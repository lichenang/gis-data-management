# Proposal: fix-minio-presigned-url

## 问题描述

GeoServer 发布影像时，后端将 MinIO 文件 URL 通过 `PUT external.geotiff` 传给 GeoServer，
GeoServer 通过 HTTP GET 下载文件到本地数据目录，然后创建 coverage store。

当前 URL 构造方式：

```java
// GeoServerCoverageStoreService.java
String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
```

这种方式存在两个问题：

1. **主机名可达性**：`minioUrl` 使用 `http://minio:9000`（Docker 内部）或 `http://localhost:9000`（本机）
   均依赖 GeoServer 和 MinIO 之间的网络连通性。IPv6 解析差异、防火墙规则等都可能导致连接失败。

2. **无认证机制**：原始 URL 不包含 MinIO 访问凭证。即使通过 `mc anonymous set download` 将桶设为公开，
   也只是临时绕过，生产环境中桶应保持私有。

## 解决方案

使用 MinIO 预签名 URL（Presigned URL）替代原始 URL：

- 后端使用 `MinioClient.getPresignedObjectUrl()` 生成带签名的 URL
- URL 中包含 AWS Signature V4 签名，GeoServer 无需任何凭证即可下载
- 签名单次有效、1 小时后过期，安全性远高于公开桶
- 使用 MinIO SDK 的 endpoint（`minio.endpoint`），与 `geoserver.minio-url` 解耦

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `ImageServiceImpl.java` | 修改 | `publishImageDataset()` 生成预签名 URL，传给 `createImageMosaicStore` |
| `GeoServerCoverageStoreService.java` | 修改 | `createImageMosaicStore` 签名改为 `(workspace, storeName, fileUrl)` |
| `GeoServerProperties.java` | 移除 | 删除 `minioUrl` 字段（不再需要） |
| `application.yml` | 移除 | 删除 `geoserver.minio-url` 配置项 |
| `application-local.yml` | 移除 | 删除 `geoserver.minio-url` 配置项 |

## 风险评估

- 低风险：预签名 URL 是 MinIO/S3 的标准功能
- `MinioClient` 已在 `ImageServiceImpl` 中注入并用于文件上传，新增预签名 URL 生成只需额外一次 SDK 调用
- `GeoServerCoverageStoreService` 不再持有 `GeoServerProperties` 依赖（移除 `minioUrl`），保持关注点分离
