# Design: fix-minio-presigned-url

## 当前架构

### 数据流（当前）

```
ImageServiceImpl.publishImageDataset()
  │
  ├── 从 raster_metadata 读取 minioBucket, minioKey
  │
  └── createImageMosaicStore(workspace, storeName, minioBucket, minioKey)
        │
        └── GeoServerCoverageStoreService
              │
              ├── fileUrl = props.minioUrl + "/" + minioBucket + "/" + minioKey
              │            = "http://localhost:9000/gis-raster/images/uuid.tif"
              │
              └── PUT external.geotiff → GeoServer → HTTP GET fileUrl → ???
```

### 问题点

- `GeoServerCoverageStoreService` 构造 URL 需要三个配置/参数：`minioUrl`、`minioBucket`、`minioKey`
- `minioUrl` 必须从 GeoServer 视角可达，且不包含认证信息
- 每次 publish 都依赖 `GeoServerProperties.minioUrl` 的配置正确性

## 目标架构

```
ImageServiceImpl.publishImageDataset()
  │
  ├── 从 raster_metadata 读取 minioBucket, minioKey
  │
  ├── presignedUrl = minioClient.getPresignedObjectUrl(
  │       Method.GET, minioBucket, minioKey, 1 hour)
  │     = "http://localhost:9000/gis-raster/images/uuid.tif?X-Amz-Signature=..."
  │       ↑ 签名自动使用 MinioClient 的 endpoint（minio.endpoint 配置）
  │       ↑ 签名包含认证信息，GeoServer 无需凭证即可下载
  │
  └── createImageMosaicStore(workspace, storeName, presignedUrl)
        │
        └── GeoServerCoverageStoreService
              │
              └── PUT external.geotiff → GeoServer → HTTP GET presignedUrl → 200 OK ✓
```

## 具体变更

### 1. ImageServiceImpl.publishImageDataset()

**新增导入**：
```java
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;
```

**在现有 minioBucket/minioKey 解析之后，调用 createImageMosaicStore 之前，生成预签名 URL**：

```java
// 生成预签名 URL（1 小时有效）
String presignedUrl = minioClient.getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(minioBucket)
        .object(minioKey)
        .expiry(1, TimeUnit.HOURS)
        .build());

// 传入预签名 URL 而非 minioBucket + minioKey
coverageStoreService.createImageMosaicStore(workspace, storeName, presignedUrl);
```

### 2. GeoServerCoverageStoreService.createImageMosaicStore()

**修改方法签名**：将 `minioBucket, minioKey` 替换为 `fileUrl`。

```java
// 修改前
public void createImageMosaicStore(String workspace, String storeName,
                                    String minioBucket, String minioKey) {
    String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
    ...
}

// 修改后
public void createImageMosaicStore(String workspace, String storeName,
                                    String fileUrl) {
    // fileUrl 已包含完整签名，直接使用
    ...
}
```

### 3. GeoServerProperties.java

删除 `minioUrl` 和 `minioBucket` 字段（`minioBucket` 在 `ImageServiceImpl` 中使用 `rasterBucket` Bean 替代）。

```java
// 删除
private String minioUrl = "http://localhost:9000";
private String minioBucket = "gis-raster";
```

### 4. application.yml / application-local.yml

删除 `geoserver.minio-url` 配置项（由 `fix-minio-url-localhost` 添加，现被此变更替代）。

## 错误处理

- 如果 `minioClient.getPresignedObjectUrl()` 失败（MinIO 不可达、凭证错误等），抛出异常，事务回滚
- 如果 GeoServer PUT 失败，`GeoServerClient` 的增强错误处理会捕获完整响应 body

## 前置条件

- `MinioClient` Bean 已正确配置（`application.yml` 中 `minio.endpoint` / `minio.accessKey` / `minio.secretKey`）
- GeoServer 可从其网络位置访问 `minio.endpoint` 的地址

## 后置条件

- `GeoServerProperties.minioUrl` 字段不再被任何代码引用，可安全移除
- `GeoServerProperties.minioBucket` 字段不再被任何代码引用，可安全移除
- 发布流程完全通过 MinIO SDK 生成签名 URL，不依赖额外配置
