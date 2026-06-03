# Proposal: fix-geoserver-file-upload

## 问题描述

GeoServer 2.28.3 在 Windows 上的影像发布始终失败，经历了三次尝试均未解决：

| 尝试 | 方式 | 结果 |
|------|------|------|
| `external.geotiff` + `http://minio:9000` | URL 引用 | 400 — 无法定位文件 |
| `external.geotiff` + `http://localhost:9000` | URL 引用（桶设为公开） | 400 — 无法定位文件 |
| `external.geotiff` + presigned URL | URL 引用（含签名） | 400 — 无法定位文件 |

**根因**：GeoServer 2.28.3 的 `RESTUtils.handleEXTERNALUpload` 在处理 HTTP URL 时，
在 Windows + Java 环境下存在兼容性问题（可能涉及 IPv6 解析、JVM HTTP 栈或 Windows 网络配置），
导致无法从 HTTP URL 下载文件。curl 验证 URL 可正常访问，证实了是 Java/GeeServer 端的 HTTP 客户端问题。

## 解决方案

放弃 `external.geotiff`（URL 引用）方式，改用 `file.geotiff`（二进制文件上传）方式：

1. 后端通过 MinIO SDK（已验证可用）从 `gis-raster` 桶下载文件
2. 将文件二进制流直接 POST/PUT 到 GeoServer 的 `file.geotiff` 端点
3. GeoServer 将文件存储到本地数据目录，创建 coverage store、coverage 和 layer

`file.geotiff` 处理流程在 GeoServer 中不经过 `handleEXTERNALUpload`，直接由文件上传处理器接管，
不存在 HTTP URL 下载问题。

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `GeoServerCoverageStoreService.java` | 重写 | `createImageMosaicStore` 改为接收 `byte[]`，使用 `file.geotiff` 上传 |
| `ImageServiceImpl.java` | 修改 | 从 MinIO 下载文件后传给 `createImageMosaicStore`，移除 presigned URL 代码 |
| `GeoServerClient.java` | 新增 | 添加 `exchangeWithBinary()` 方法支持二进制 PUT（Content-Type: image/tiff） |

## 风险评估

- 低风险：MinIO SDK 的 `getObject()` 已在文件上传流程中使用，`file.geotiff` 是标准 REST API
- 无需外部 URL 可达性，不受 IPv6、代理、防火墙影响
- 文件通过后端流转，后端已有 MinIO 认证凭证
- 30MB 文件在后端内存中中转，对现代 Java 应用无压力
