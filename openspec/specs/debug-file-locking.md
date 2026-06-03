# 诊断报告：GeoServer 文件锁定问题

## 问题描述

GeoServer 发布影像时出现 "unable to remove existing" 错误，文件被占用导致发布失败。

错误信息：
```
unable to remove existing file 'raster_40.geotiff' for coverage store create
```

## 错误分析

### 1. 错误发生场景

重新发布同一个数据集（相同的 storeName，如 `raster_40`）时更容易出现此错误。

### 2. 根因分析

经过代码分析，发现问题不在于 Java 端的文件流未关闭，而是**发布流程缺少删除旧 store 的步骤**。

```
当前流程:
┌────────────────────────────────────────────────────────────┐
│ 1. 上传文件到 MinIO                                        │
│ 2. 解析 GeoTIFF 元数据                                     │
│    └─ GeoTiffParser.parse() ✓ 使用 try-with-resources     │
│ 3. 从 MinIO 下载                                           │
│    └─ try (InputStream stream = ...) ✓ 使用 try-with      │
│ 4. 直接上传到 GeoServer                                    │
│    └─ coverageStoreService.createImageMosaicStore()       │
│       ⚠️ 没有检查/删除已存在的 store                        │
└────────────────────────────────────────────────────────────┘
```

当 GeoServer 已存在同名 coverage store 时：
- GeoServer 尝试删除旧文件
- 但文件可能被 GeoServer 内部锁定（如之前的发布操作未完全清理）
- 或存在相关的 lock 文件

### 3. Java 端代码分析

#### 3.1 GeoTiffParser (正确 - 使用 try-with-resources)

```java
// ImageServiceImpl.java:138 - 正确关闭流
RasterMetadata rasterMetadata = GeoTiffParser.parse(file);

// GeoTiffParser.java:43-143
try (InputStream inputStream = file.getInputStream()) {
    GeoTiffReader reader = new GeoTiffReader(inputStream);
    GridCoverage2D coverage = reader.read(null);
    // ...
    reader.dispose();  // 在 try 块内
}  // InputStream 自动关闭
```

#### 3.2 MinIO 下载 (正确 - 使用 try-with-resources)

```java
// ImageServiceImpl.java:259-268 - 正确关闭流
try (InputStream stream = minioClient.getObject(
        GetObjectArgs.builder()
            .bucket(minioBucket)
            .object(minioKey)
            .build())) {
    fileData = stream.readAllBytes();
}
```

#### 3.3 GeoServer 上传 (问题所在)

```java
// ImageServiceImpl.java:270 - 没有先删除旧 store
coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);

// GeoServerCoverageStoreService.java:26-38
public void createImageMosaicStore(String workspace, String storeName, byte[] fileData) {
    String endpoint = "/rest/workspaces/" + workspace +
        "/coveragestores/" + storeName +
        "/file.geotiff?configure=first&coverageName=" + storeName;
    // ⚠️ 直接 PUT，不检查 store 是否已存在
    client.exchangeWithBinary(endpoint, HttpMethod.PUT, fileData);
}
```

## 修复方案

### 方案 1: 在创建前先删除旧 store（推荐）

修改 `ImageServiceImpl.publishImage()` 方法，在上传前先尝试删除已存在的 store：

```java
// 在 coverageStoreService.createImageMosaicStore() 之前添加
try {
    coverageStoreService.deleteStore(workspace, storeName);
    log.info("Deleted existing coverage store: {}", storeName);
} catch (Exception e) {
    log.warn("Failed to delete existing store (may not exist): {}", storeName, e);
}

// 然后创建新 store
coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);
```

### 方案 2: 使用 GeoServer REST API 的 PUT 模式覆盖

确认 GeoServer 的 `file.geotiff` 端点支持覆盖模式：
- 当前: `PUT /rest/workspaces/{ws}/coveragestores/{store}/file.geotiff`
- 参数: `?configure=first&coverageName={store}`

如果支持，应该可以自动覆盖。问题可能在于：
1. 文件被锁定
2. 需要先 truncate/seed

### 方案 3: 使用 truncate API 清理后再上传

在上传前先调用 GWC truncate API：

```java
// 先截断缓存
try {
    cacheService.truncateLayer(workspace, layerName);
} catch (Exception e) {
    log.warn("Failed to truncate layer: {}", e.getMessage());
}

// 再删除 store
try {
    coverageStoreService.deleteStore(workspace, storeName);
} catch (Exception e) {
    log.warn("Store may not exist: {}", e.getMessage());
}

// 最后创建新 store
coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);
```

## 验证步骤

### 手动测试

1. **删除已存在的 store**:
```bash
curl -X DELETE -u admin:geoserver \
  http://127.0.0.1:8080/geoserver/rest/workspaces/gisplatform/coveragestores/raster_40
```

2. **重新发布影像**（应该成功）

3. **再次发布同一影像**（如果不删除，会失败）

### 修复后的预期行为

修复后，再次发布同一影像时应该：
1. 先删除旧的 coverage store
2. 等待删除完成
3. 创建新的 coverage store
4. 成功发布

## 相关代码文件

- `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java` - publishImage() 方法
- `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCoverageStoreService.java` - createImageMosaicStore()
- `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerClient.java` - exchangeWithBinary()

## 推荐修复代码

```java
// ImageServiceImpl.java - publishImage() 方法中，在 createImageMosaicStore 之前添加：

// 先删除已存在的 coverage store
try {
    coverageStoreService.deleteStore(workspace, storeName);
    log.info("Deleted existing coverage store: {}", storeName);
    // 等待一下让 GeoServer 清理完成
    Thread.sleep(1000);
} catch (Exception e) {
    log.info("No existing coverage store to delete: {}", storeName);
}

// 然后创建新的 coverage store
coverageStoreService.createImageMosaicStore(workspace, storeName, fileData);
```
