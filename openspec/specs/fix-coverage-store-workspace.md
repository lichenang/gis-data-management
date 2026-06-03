# Fix: Coverage Store "Store must be part of a workspace" 错误

## 问题现象

创建 coverage store 时 GeoServer 返回：
```
Store must be part of a workspace
```

当前使用两步法（方案 B）：
- **Step 1:** `POST /rest/workspaces/gisplatform/coveragestores` — 创建空 store ← **失败位置**
- Step 2: `PUT .../external.geotiff?configure=first` — 配置 URL

## 根因分析

### 当前 Step 1 发出的 XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<coverageStore>
  <name>raster_27</name>
  <enabled>true</enabled>
</coverageStore>
```

GeoServer 2.28.2 `CoverageStoreController.coverageStorePost` 的源码：

```java
@PostMapping(consumes = {MediaType.APPLICATION_XML_VALUE, ...})
public ResponseEntity<String> coverageStorePost(
        @RequestBody CoverageStoreInfo coverageStore,  // ← 从 XML 反序列化
        @PathVariable String workspaceName,             // ← 从 URL 获取，但未绑定到对象
        UriComponentsBuilder builder) {
    catalog.validate(coverageStore, true).throwIfInvalid();  // ← 校验失败
    catalog.add(coverageStore);
```

**关键问题：** URL 路径中的 `workspaceName` 没有被绑定到反序列化后的 `CoverageStoreInfo` 对象上。由于 XML 中没有 `<workspace>` 元素，`coverageStore.getWorkspace()` 为 null，导致 `catalog.validate()` 失败。

```
请求:
  POST /rest/workspaces/gisplatform/coveragestores
  Body: <coverageStore><name>raster_27</name><enabled>true</enabled></coverageStore>
                                                      ↑
                                              workspace = null → 校验失败

修复后:
  POST /rest/workspaces/gisplatform/coveragestores
  Body: <coverageStore><name>raster_27</name><workspace><name>gisplatform</name></workspace><enabled>true</enabled></coverageStore>
                                                      ↑
                                              workspace = "gisplatform" → 校验通过
```

### 验证问题 1：XML 中是否缺少 `<workspace>`？

**是。** 当前构造的空 store XML 不包含 `<workspace>` 标签。

正确格式应为嵌套对象引用：
```xml
<workspace>
  <name>gisplatform</name>
</workspace>
```

GeoServer XStream 在反序列化时将 `<workspace><name>...</name></workspace>` 映射为一个 `WorkspaceInfoImpl` 对象，其 name 属性被设置为指定的值。然后 `catalog.validate()` 从 name 查找真实 workspace 对象进行校验。

### 验证问题 2：是否缺少命名空间声明？

**否。** GeoServer REST API 的 XStream 序列化器不需要 XML 命名空间声明。提交 `<?xml version="1.0" encoding="UTF-8"?>` 加原生标签即可。GeoServer 文档和示例中均不包含 `xmlns` 属性。

### 验证问题 3：类型参数是否正确？

**不适用。** Step 1 创建的是空 store，XML 中不包含 `<type>` 元素，这是正确的。类型在 Step 2（PUT external.geotiff）中通过 `{format}` 路径参数指定。

## 修正方案

### 方案 A：加 `<workspace>` 到 Step 1 XML（推荐，最小改动）

```java
String emptyStoreXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    "<coverageStore>" +
    "<name>" + storeName + "</name>" +
    "<workspace>" +
      "<name>" + workspace + "</name>" +
    "</workspace>" +
    "<enabled>true</enabled>" +
    "</coverageStore>";
```

### 方案 B：移除 Step 1，Step 2 的 PUT 自带创建功能（更简洁）

从 `CoverageStoreFileController.coverageStorePut` 源码：

```java
CoverageStoreInfo info = catalog.getCoverageStoreByName(workspaceName, storeName);
boolean add = false;
if (info == null) {
    CatalogBuilder builder = new CatalogBuilder(catalog);
    builder.setWorkspace(catalog.getWorkspaceByName(workspaceName));  // ← workspace 从 URL 绑定
    info = builder.buildCoverageStore(storeName);                      // ← 创建 store
    add = true;
}
// ... 后续设置 URL、自动发布 coverage + layer
```

PUT handler 已处理好 workspace 绑定，不需要在请求体中指定。因此可以直接跳过 Step 1，仅用 Step 2 完成 store 创建 + URL 配置 + 图层发布。

```java
// 删除 Step 1 的 POST 代码，直接执行 Step 2
String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
String externalEndpoint = "/rest/workspaces/" + workspace +
    "/coveragestores/" + storeName +
    "/external.geotiff?configure=first&coverageName=" + storeName;

try {
    client.exchangeWithContentType(externalEndpoint, HttpMethod.PUT,
        fileUrl, String.class, MediaType.TEXT_PLAIN);
    log.info("Coverage store {} created and configured with URL: {}", storeName, fileUrl);
} catch (Exception e) {
    throw new RuntimeException("Failed to create coverage store: " + storeName +
        ". URL: " + fileUrl, e);
}
```

### 方案对比

| 方面 | 方案 A：加 workspace | 方案 B：移除 Step 1 |
|------|---------------------|-------------------|
| 改动量 | 1 行 XML 构造 | 删除 ~10 行代码 |
| 风险 | 低，只是补全字段 | 低，PUT handler 已有 create-if-not-exists |
| 一致性 | 仍然两步，API 调用次数不变 | 一步完成，减少一次 HTTP 调用 |
| 测试 | 需验证 POST 返回 201 | 需验证 PUT 直接返回 201 |

## 完整修正代码（方案 B 推荐）

```java
public void createImageMosaicStore(String workspace, String storeName,
                                    String minioBucket, String minioKey) {
    if (storeExists(workspace, storeName)) {
        log.info("CoverageStore {} already exists, skipping", storeName);
        return;
    }

    String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
    String endpoint = "/rest/workspaces/" + workspace +
        "/coveragestores/" + storeName +
        "/external.geotiff?configure=first&coverageName=" + storeName;

    log.info("Creating coverage store via PUT {} -> {}", endpoint, fileUrl);
    try {
        client.exchangeWithContentType(endpoint, HttpMethod.PUT,
            fileUrl, String.class, MediaType.TEXT_PLAIN);
        log.info("Coverage store {} created and configured with URL: {}", storeName, fileUrl);
    } catch (Exception e) {
        throw new RuntimeException("Failed to create coverage store: " + storeName +
            ". URL: " + fileUrl, e);
    }
}
```

## curl 验证命令

### 方案 A 验证

```bash
curl -u admin:geoserver -X POST \
  -H "Content-Type: application/xml" \
  -d '<coverageStore><name>test_store</name><workspace><name>gisplatform</name></workspace><enabled>true</enabled></coverageStore>' \
  'http://localhost:8080/geoserver/rest/workspaces/gisplatform/coveragestores'
```

预期：返回 201 Created，Location 头指向新创建的 store。

### 方案 B 验证

```bash
curl -u admin:geoserver -X PUT \
  -H "Content-Type: text/plain" \
  -d 'file:///tmp/test.tif' \
  'http://localhost:8080/geoserver/rest/workspaces/gisplatform/coveragestores/test_store/external.geotiff?configure=first&coverageName=test_store'
```

预期：返回 201 Created，coverage store + coverage + layer 全部自动创建。
