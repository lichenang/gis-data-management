# Design: fix-remove-empty-store-step

## 当前代码分析

### 当前 createImageMosaicStore

```java
public void createImageMosaicStore(String workspace, String storeName,
                                    String minioBucket, String minioKey) {
    // Step 1: 检查并创建空 store（有缺陷）
    if (!storeExists(workspace, storeName)) {       // GET 检查
        createEmptyStore(workspace, storeName);     // POST 创建 ← 始终失败
    }

    // Step 2: PUT external URL 配置 store + 发布
    String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
    String endpoint = "/rest/workspaces/" + workspace + "/coveragestores/"
        + storeName + "/external.geotiff?configure=first&coverageName=" + storeName;
    client.exchangeWithContentType(endpoint, HttpMethod.PUT,
        fileUrl, String.class, MediaType.TEXT_PLAIN);
}
```

### 辅助方法

```java
private boolean storeExists(String workspace, String storeName) { ... }
private void createEmptyStore(String workspace, String storeName) {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<coverageStore>" +
        "<name>" + storeName + "</name>" +
        "<enabled>true</enabled>" +
        "</coverageStore>";
    client.post(endpoint, xml, String.class);  // ← 缺少 workspace → 500
}
```

## 修复方案

移除 `createEmptyStore()` 和 `storeExists()` 方法，直接执行 PUT：

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

### PUT handler 自动完成的事务

1. 查找 workspace（从 URL）→ `catalog.getWorkspaceByName(workspaceName)`
2. 查找 store → 不存在时用 `CatalogBuilder` 创建并 `catalog.add(info)`
3. 设置 `CoverageStoreInfo.setURL(fileUrl)` → 指向外部文件
4. `configure=first` → 自动创建 Coverage + Layer
5. 匹配的 `coverageName` 参数确保 coverage 名称与 store 一致

### 移除的代码

| 方法 | 行数 | 原因 |
|------|------|------|
| `createEmptyStore()` | ~12 行 | POST 空 store 始终失败（缺少 workspace） |
| `storeExists()` | ~15 行 | PUT handler 自带 create-if-not-exists，无需前置检查 |
| 相关 import | 若干 | `HttpHeaders`、`MediaType.APPLICATION_XML` 等不再需要 |

## 错误处理

- PUT 成功 → 201 Created → 方法正常返回
- PUT 失败 → 异常被 `GeoServerClient` 的增强错误处理捕获，包含完整响应 body
- 调用方（`ImageServiceImpl.publishImageDataset()`）已有 try-catch 包装

## 前置条件

- `GeoServerClient.exchangeWithContentType()` 已支持 `MediaType.TEXT_PLAIN`
- MinIO HTTP URL 格式已正确配置

## 后置条件

- 新的发布请求会直接创建 coverage store、coverage 和 layer
- 重复发布（同一 storeName）时 PUT handler 会更新已有 store 的 URL（幂等行为）
