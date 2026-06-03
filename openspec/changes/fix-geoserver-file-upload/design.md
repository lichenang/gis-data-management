# Design: fix-geoserver-file-upload

## 新旧流程对比

### 当前流程（external.geotiff — 有缺陷）

```
ImageServiceImpl                  GeoServerCoverageStoreService
      │                                    │
      ├─ minioClient.getPresignedObjectUrl │
      │   → presigned URL                  │
      │                                    │
      └─ createImageMosaicStore(url) ──────┤
                                           ├─ PUT .../external.geotiff
                                           │   Content-Type: text/plain
                                           │   Body: presigned URL
                                           │
                                           └─ GeoServer.handleEXTERNALUpload()
                                               → HTTP GET presigned URL  ← 失败
```

### 新流程（file.geotiff — 可靠）

```
ImageServiceImpl                  GeoServerCoverageStoreService
      │                                    │
      ├─ minioClient.getObject()           │
      │   → InputStream (with auth)        │
      │                                    │
      ├─ readAllBytes() → byte[]           │
      │                                    │
      └─ createImageMosaicStore(bytes) ────┤
                                           ├─ PUT .../file.geotiff
                                           │   Content-Type: image/tiff
                                           │   Body: 文件二进制数据
                                           │
                                           └─ GeoServer CoverageStoreFileController
                                               → 保存文件到本地数据目录 ✓
                                               → configure=first 创建 coverage ✓
                                               → 自动发布 layer ✓
```

## 详细变更

### 1. GeoServerCoverageStoreService.java

方法签名从 `(workspace, storeName, fileUrl)` 改为 `(workspace, storeName, fileData)`：

```java
/**
 * @param workspace  工作区名称
 * @param storeName  coverage store 名称
 * @param fileData   GeoTIFF 文件二进制数据
 */
public void createImageMosaicStore(String workspace, String storeName,
                                    byte[] fileData) {
    String endpoint = "/rest/workspaces/" + workspace +
        "/coveragestores/" + storeName +
        "/file.geotiff?configure=first&coverageName=" + storeName;

    log.info("Uploading GeoTIFF to coverage store: {}", storeName);
    try {
        client.exchangeWithBinary(endpoint, HttpMethod.PUT, fileData);
        log.info("Coverage store {} created/configured via file upload", storeName);
    } catch (Exception e) {
        throw new RuntimeException("Failed to upload GeoTIFF for coverage store: " +
            storeName, e);
    }
}
```

### 2. GeoServerClient.java — 新增 exchangeWithBinary

当前 `exchangeWithContentType` 用于文本/XML body。上传二进制文件需要新方法：

```java
public void exchangeWithBinary(String path, HttpMethod method, byte[] body) {
    String url = props.getUrl() + path;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_TIFF);
    String auth = props.getUsername() + ":" + props.getPassword();
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    headers.set("Authorization", "Basic " + encodedAuth);

    HttpEntity<byte[]> request = new HttpEntity<>(body, headers);

    try {
        ResponseEntity<String> response = restTemplate.exchange(
            url, method, request, String.class);
        log.debug("GeoServer binary upload response: {}", response.getStatusCode());
    } catch (HttpStatusCodeException e) {
        String errorBody = e.getResponseBodyAsString();
        log.error("GeoServer binary upload error ({}): {}", e.getStatusCode(), errorBody);
        String geoServerMsg = extractGeoServerErrorMessage(errorBody);
        throw new RuntimeException("GeoServer binary upload failed: " + url +
            ", status: " + e.getStatusCode() +
            ", error: " + geoServerMsg, e);
    }
}
```

注意：`MediaType.IMAGE_TIFF` 在 Spring 5 中可能不存在，需自定义：
```java
private static final MediaType IMAGE_TIFF = MediaType.valueOf("image/tiff");
```

### 3. ImageServiceImpl.publishImageDataset()

将 presigned URL 生成替换为 MinIO 文件下载：

```java
// 替换这部分代码（lines 249-262）：
// 从
//     String presignedUrl = minioClient.getPresignedObjectUrl(...);
//     coverageStoreService.createImageMosaicStore(workspace, storeName, presignedUrl);
// 改为

// 从 MinIO 下载文件
byte[] fileData;
try (InputStream stream = minioClient.getObject(
        GetObjectArgs.builder()
            .bucket(minioBucket)
            .object(minioKey)
            .build())) {
    fileData = stream.readAllBytes();
} catch (Exception e) {
    throw new RuntimeException("Failed to download file from MinIO: " +
        minioBucket + "/" + minioKey, e);
}
log.info("Downloaded {} bytes from MinIO: {}/{}", fileData.length, minioBucket, minioKey);

// 上传到 GeoServer
coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);
```

新增导入：
```java
import io.minio.GetObjectArgs;
```

移除不再需要的导入：
```java
// 移除
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;
```

## 错误处理

- MinIO 下载失败：`getObject()` 抛出异常 → `ImageServiceImpl` 捕获并包装为 RuntimeException
- GeoServer 上传失败：`exchangeWithBinary()` 的 HttpStatusCodeException 捕获完整响应 body
- 事务回滚：`@Transactional(rollbackFor = Exception.class)` 确保原子性

## 前置条件

- `MinioClient` Bean 已正确配置且有访问 `gis-raster` 桶的权限
- GeoServer REST API 的 `file.geotiff` 端点可正常处理二进制上传

## 后置条件

- `external.geotiff` 相关代码全部移除
- presigned URL 生成代码不再需要
- `GeoServerProperties.minioUrl` 字段已不再被任何代码引用
