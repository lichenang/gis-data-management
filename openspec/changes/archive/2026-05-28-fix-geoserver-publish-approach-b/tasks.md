# Tasks: fix-geoserver-publish-approach-b

## Task 1: GeoServerProperties 添加 MinIO 配置字段

**文件**: `backend/src/main/java/com/gisplatform/config/GeoServerProperties.java`

在现有字段后添加：
```java
private String minioUrl = "http://minio:9000";
private String minioBucket = "gis-raster";
```

并在 `application.yml` 中添加对应配置：
```yaml
geoserver:
  minio-url: ${MINIO_ENDPOINT:http://minio:9000}
  minio-bucket: ${MINIO_BUCKET_RASTER:gis-raster}
```

## Task 2: GeoServerClient 改进错误处理

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerClient.java`

- 添加 `org.springframework.web.client.HttpClientErrorException` 和 `org.springframework.web.client.HttpServerErrorException` 导入
- 在 `exchange` 方法中添加 `catch (HttpStatusCodeException e)` 块（在通用 `catch (Exception e)` 之前）
- 调用 `e.getResponseBodyAsString()` 获取响应体并记录日志
- 添加 `extractGeoServerErrorMessage(xml)` 辅助方法从 `<message>` 标签提取错误信息
- 添加 `exchangeWithContentType(String path, HttpMethod method, Object body, Class<T> responseType, MediaType contentType)` 重载方法以支持 PUT 请求的 `text/plain` Content-Type

## Task 3: 重写 GeoServerCoverageStoreService

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCoverageStoreService.java`

关键变更：

### 方法签名变更
```java
// 原来
public void createImageMosaicStore(String workspace, String storeName, 
    String minioKey, int minZoom, int maxZoom)

// 改为
public void createImageMosaicStore(String workspace, String storeName, 
    String minioBucket, String minioKey)
```
- 移除未使用的 `minZoom`、`maxZoom` 参数
- 添加 `minioBucket` 参数

### 实现逻辑
1. 检查 store 是否已存在（`storeExists()`）
2. **Step 1 - POST 空 store**：
   - Endpoint: `POST /rest/workspaces/{ws}/coveragestores`
   - Body (XML): `<coverageStore><name>{store}</name><enabled>true</enabled></coverageStore>`
   - Content-Type: `application/xml`
3. **Step 2 - PUT external URL**：
   - Endpoint: `PUT /rest/workspaces/{ws}/coveragestores/{store}/external.geotiff?configure=first&coverageName={store}`
   - Body (text/plain): `http://{minioUrl}/{bucket}/{key}`
   - Content-Type: `text/plain`
4. PUT 调用使用 `exchangeWithContentType()` 并指定 `MediaType.TEXT_PLAIN`

### 移除
- 移除 `private GeoServerProperties props` 注入（不再需要，因为 minioUrl/minioBucket 通过参数传入）

## Task 4: 修改 ImageServiceImpl.publishImageDataset

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

### 变更内容

在 `publishImageDataset()` 方法中：

1. **添加 raster_metadata 查询**（在现有校验之后）：
   ```java
   LambdaQueryWrapper<RasterMetadata> queryWrapper = new LambdaQueryWrapper<>();
   queryWrapper.eq(RasterMetadata::getDatasetId, id);
   RasterMetadata rasterMetadata = rasterMetadataMapper.selectOne(queryWrapper);
   if (rasterMetadata == null) {
       throw new RuntimeException("raster_metadata 不存在");
   }
   ```

2. **获取 minioBucket 和 minioKey**：
   ```java
   String minioBucket = rasterMetadata.getMinioBucket();
   if (minioBucket == null || minioBucket.isEmpty()) {
       minioBucket = geoServerProperties.getMinioBucket();
   }
   String minioKey = rasterMetadata.getMinioKey();
   ```

3. **调用覆盖率 store 服务**（传递 bucket + key）：
   ```java
   coverageStoreService.createImageMosaicStore(workspace, storeName, 
       minioBucket, minioKey);
   ```

4. **移除 layerService.publishLayer() 调用**（PUT external.geotiff?configure=first 已自动发布）

## Task 5: 验证编译

**命令**:
```bash
cd backend && mvn compile
```

确认无编译错误。

## Task 6: 端到端验证

### 前置条件
- GeoServer 2.28.x 运行中（`http://localhost:8080/geoserver`）
- MinIO 运行中（`http://localhost:9000`）
- MinIO bucket `gis-raster` 中存在测试 GeoTIFF 文件

### 验证步骤
1. 启动后端服务
2. 调用 `POST /api/v1/images/{id}/publish` 发布一个已上传的影像
3. 检查后端日志：确认 Step 1 POST 成功 + Step 2 PUT 成功
4. 验证 GeoServer：
   ```bash
   curl -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/gisplatform/coveragestores/raster_{id}.xml
   ```
   返回的 XML 中 `<url>` 应为 `http://minio:9000/gis-raster/images/{uuid}.tif`
5. 验证图层已发布：
   ```bash
   curl -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/gisplatform/layers.xml
   ```
   应包含 `raster_{id}` 图层
6. 通过 WMS 访问验证：`http://localhost:8080/geoserver/gisplatform/wms?service=WMS&request=GetMap&layers=gisplatform:raster_{id}&format=image/png`
