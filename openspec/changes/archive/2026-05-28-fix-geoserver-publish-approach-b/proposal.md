# Proposal: fix-geoserver-publish-approach-b

## 问题描述

影像发布时 `GeoServerCoverageStoreService.createImageMosaicStore()` 调用失败，错误信息为：

```
Failed to create coverage store: raster_27
```

通过分析 GeoServer 2.28.2 源码（`CoverageStoreController` 和 `CoverageStoreFileController`），发现以下问题：

| 问题 | 位置 | 严重程度 |
|------|------|---------|
| **错误的 URL 协议** | `GeoServerCoverageStoreService.java:28` | 高 - 使用 `s3://minio/` 但 GeoServer 未安装 S3 插件 |
| **错误的 coverage type** | `GeoServerCoverageStoreService.java:34` | 中 - 单 GeoTIFF 使用 `ImageMosaic` 类型 |
| **未捕获 GeoServer 错误响应** | `GeoServerClient.java:38-40` | 中 - 丢失 `HttpStatusCodeException.getResponseBodyAsString()` |
| **未传递 bucket 名称** | `ImageServiceImpl.java:230` | 高 - minioBucket 未从 raster_metadata 获取 |
| **缺失 MinIO 配置** | `GeoServerProperties.java` | 中 - 缺少 minioUrl、minioBucket 字段 |
| **密码默认值为空** | `application.yml` | 低 - `GEOSERVER_PASSWORD:` 无默认值 |

## 目标

采用方案 B（两步法）修复影像发布流程：

1. **POST** `/workspaces/{ws}/coveragestores` — 创建空的 coverage store（保留现有逻辑）
2. **PUT** `/workspaces/{ws}/coveragestores/{store}/external.geotiff?configure=first&coverageName={store}` — 配置外部文件 URL 并自动发布图层
3. 移除后续独立的 `layerService.publishLayer()` 调用（PUT 已自动完成）
4. 修正 MinIO URL 从 `s3://` 改为 `http://` 格式
5. 捕获 GeoServer XML 错误响应并记录日志

## 影响范围

**后端修改文件：**

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `GeoServerProperties.java` | 修改 | 添加 minio-url、minio-bucket 字段 |
| `GeoServerClient.java` | 修改 | 捕获 `HttpStatusCodeException.getResponseBodyAsString()` |
| `GeoServerCoverageStoreService.java` | 重写 | 改为两步法：POST 空 store + PUT external.geotiff；签名增加 minioBucket 参数 |
| `ImageServiceImpl.java` | 修改 | 从 raster_metadata 读取 minioBucket；移除 publishLayer 调用 |

**不涉及：**
- 前端代码
- 数据库表结构
- 现有矢量发布流程
- 切片/缓存相关代码

## 风险评估

- 低风险：两步法基于 GeoServer 2.28.x 标准 REST API，每个步骤独立可验证
- 低风险：PUT external.geotiff 的 create-if-not-exists 行为有源码保障
- 若 PUT 步骤失败，空 store 可能残留（需手动清理），但不会影响现有数据
