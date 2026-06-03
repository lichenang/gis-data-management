# Design: fix-geoserver-publish-approach-b

## 1. 变更后的发布流程

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    方案 B：两步法发布流程                                 │
└──────────────────────────────────────────────────────────────────────────┘

Step 1: POST /rest/workspaces/gisplatform/coveragestores (create empty store)
─────────────────────────────────────────────────────────────────────────
  Body:
  <coverageStore>
    <name>raster_27</name>
    <enabled>true</enabled>
  </coverageStore>

  → 201 Created（store 已存在则跳过）


Step 2: PUT /rest/workspaces/gisplatform/coveragestores/raster_27/ \
           external.geotiff?configure=first&coverageName=raster_27
─────────────────────────────────────────────────────────────────────────
  Body (text/plain): http://minio:9000/gis-raster/images/uuid.tif
  Content-Type: text/plain

  → 201 Created
  → GeoServer 自动：创建/更新 store → 设置 URL → 创建 coverage → 发布 layer


结果：一次 PUT 调用替代了原有的 store 创建 + layer 发布两步
```

## 2. GeoServer REST API 调用验证

基于 GeoServer 2.28.2 源码 `CoverageStoreFileController` 确认：

| 行为 | POST external.geotiff | PUT external.geotiff |
|------|----------------------|----------------------|
| 如果 store 不存在 | 返回 404 | 自动创建 store |
| 如果 store 已存在 | 收获 granule（仅 ImageMosaic） | 更新 store 配置 |
| 自动发布 layer | 否 | 是（`configure=first`） |
| Body 格式 | 由 UploadMethod 决定 | text/plain（URL 字符串） |

PUT handler 关键代码（`CoverageStoreFileController.java:84-130`）：
```java
CoverageStoreInfo info = catalog.getCoverageStoreByName(workspaceName, storeName);
boolean add = false;
if (info == null) {
    info = builder.buildCoverageStore(storeName);  // 自动创建
    add = true;
}
info.setType(coverageFormat.getName());            // 从 format 参数获取
// 对于 external 方法，URL 直接从请求体读取
info.setURL(uploadedFileURL.toExternalForm());     
```

## 3. 配置变更

### GeoServerProperties.java 添加字段

```java
private String minioUrl = "http://minio:9000";
private String minioBucket = "gis-raster";
```

对应 `application.yml`：
```yaml
geoserver:
  minio-url: ${MINIO_ENDPOINT:http://minio:9000}
  minio-bucket: ${MINIO_BUCKET_RASTER:gis-raster}
```

## 4. GeoServerClient 改进

### 错误响应捕获

当前代码：
```java
} catch (Exception e) {
    throw new RuntimeException("GeoServer API call failed: " + url + ", error: " + e.getMessage(), e);
}
```

改为捕获 `HttpStatusCodeException` 提取响应体：
```java
} catch (HttpStatusCodeException e) {
    String errorBody = e.getResponseBodyAsString();
    log.error("GeoServer error ({}): {}", e.getStatusCode(), errorBody);
    String geoServerMsg = extractGeoServerErrorMessage(errorBody);
    throw new RuntimeException("GeoServer API failed: " + url +
        ", status: " + e.getStatusCode() +
        ", GeoServer error: " + geoServerMsg, e);
}
```

提取方法 `extractGeoServerErrorMessage(xml)` 从 `<message>` 标签解析。

### 支持 text/plain Content-Type

PUT 请求需要 Content-Type: `text/plain`（与当前默认的 `application/xml` 不同）。
在 `GeoServerClient` 中添加 `exchangeWithContentType()` 方法或重载。

## 5. CoverageStoreService 重构

### 当前方法签名

```java
public void createImageMosaicStore(String workspace, String storeName, 
    String minioKey, int minZoom, int maxZoom)
```

### 新方法签名和实现

```java
public void createImageMosaicStore(String workspace, String storeName,
    String minioBucket, String minioKey)
```

实现逻辑：

```
┌──────────────────────────────────────────────────────────┐
│ createImageMosaicStore(ws, store, bucket, key)           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  1. if storeExists(ws, store) → 已存在，跳过              │
│                                                          │
│  2. Step 1: POST empty store                              │
│     POST /rest/workspaces/{ws}/coveragestores             │
│     Body (XML): <coverageStore>...</coverageStore>         │
│                                                          │
│  3. Step 2: PUT external URL with auto-publish            │
│     PUT /rest/workspaces/{ws}/coveragestores/{store}/     │
│         external.geotiff?configure=first                  │
│         &coverageName={store}                             │
│     Body (text/plain): http://minio:9000/{bucket}/{key}   │
│     Content-Type: text/plain                               │
│                                                          │
│  4. 更新 dataset 的 tile_status = "pending"              │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## 6. ImageServiceImpl 调整

### publishImageDataset() 变更

```java
// 之前：
String minioKey = dataset.getMinioKey();  // 只有 key，没有 bucket
coverageStoreService.createImageMosaicStore(workspace, storeName, 
    minioKey, minZoom, maxZoom);
layerService.publishLayer(workspace, storeName, layerName, ...);

// 之后：
RasterMetadata rm = rasterMetadataMapper.selectByDatasetId(id);
String minioBucket = rm.getMinioBucket() != null ? 
    rm.getMinioBucket() : props.getMinioBucket();
String minioKey = rm.getMinioKey() != null ? 
    rm.getMinioKey() : dataset.getMinioKey();
coverageStoreService.createImageMosaicStore(workspace, storeName, 
    minioBucket, minioKey);
// layerService.publishLayer() 不再需要 - PUT external.geotiff?configure=first 自动完成
```

## 7. 错误处理

| 场景 | 行为 |
|------|------|
| Step 1 POST 失败 | 原样抛出异常（GeoServer 错误信息已提取到日志） |
| Step 2 PUT 失败 | 空 store 可能残留，记录警告日志，抛出异常 |
| store 已存在但 step 2 失败 | PUT handler 会更新现有 store URL，不会重复创建 |
| MinIO HTTP URL 不可访问 | GeoServer 在 PUT 时尝试读取文件，返回错误（captured 在日志） |

## 8. 前置/后置条件

**前置：**
- GeoServer 2.28.x 运行中且 REST API 可访问
- MinIO 服务运行中且 GeoServer 可通过 HTTP 访问 `http://minio:9000`
- raster_metadata 表已包含 minioBucket、minioKey 字段

**后置：**
- GeoServer 中创建了 coverage store + coverage + layer
- Dataset.tile_status 设为 "pending"（等待后续切片任务）
- Dataset.wms_url / wmts_url 已更新
