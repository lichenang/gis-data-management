# Tasks: fix-remove-empty-store-step

## Task 1: 移除 storeExists 和 createEmptyStore，简化 createImageMosaicStore

- [x] done

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCoverageStoreService.java`

将 `createImageMosaicStore` 替换为仅包含 Step 2 PUT 的直接实现：

```java
// 修改前
public void createImageMosaicStore(String workspace, String storeName,
                                    String minioBucket, String minioKey) {
    if (!storeExists(workspace, storeName)) {
        createEmptyStore(workspace, storeName);
    }
    String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
    String endpoint = "/rest/workspaces/" + workspace + "/coveragestores/"
        + storeName + "/external.geotiff?configure=first&coverageName=" + storeName;
    client.exchangeWithContentType(endpoint, HttpMethod.PUT,
        fileUrl, String.class, MediaType.TEXT_PLAIN);
}
```

替换为：

```java
public void createImageMosaicStore(String workspace, String storeName,
                                    String minioBucket, String minioKey) {
    String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
    String endpoint = "/rest/workspaces/" + workspace
        + "/coveragestores/" + storeName
        + "/external.geotiff?configure=first&coverageName=" + storeName;
    log.info("Creating coverage store via PUT: {} -> {}", endpoint, fileUrl);
    try {
        client.exchangeWithContentType(endpoint, HttpMethod.PUT,
            fileUrl, String.class, MediaType.TEXT_PLAIN);
    } catch (Exception e) {
        throw new RuntimeException("Failed to create coverage store: "
            + storeName + ". URL: " + fileUrl, e);
    }
}
```

同时删除 `createEmptyStore()` 和 `storeExists()` 两个私有方法，并清理不再使用的 import。

## Task 2: 验证编译

**命令**:
```bash
cd backend && mvn compile
```

确认无编译错误。

- [x] done
