# Fix: GeoServer 2.28.3 Coverage Store API

## 问题诊断

当前实现存在以下问题：

| 问题 | 位置 | 说明 |
|------|------|------|
| 错误的 API 路径 | GeoServerCoverageStoreService | 使用 `/coveragestores` 而非 `/coveragestores/{name}/external.geotiff` |
| 错误的 URL 协议 | GeoServerCoverageStoreService | 使用 `s3://minio/` 而非 HTTP URL |
| 未传递 bucket 名称 | ImageServiceImpl.publishImageDataset | 只传递了 minioKey，缺少 bucket |
| 未捕获 GeoServer 错误响应 | GeoServerClient | 只获取异常消息，丢失详细 XML 错误 |

## GeoServer 2.28.3 REST API 正确用法

### 创建外部 GeoTIFF Coverage Store

**正确 endpoint:**
```
POST /rest/workspaces/{workspace}/coveragestores/{storeName}/external.geotiff
```

**请求体格式 (XML):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<coverageStore>
  <name>{storeName}</name>
  <type>GeoTIFF</type>
  <url>{fileURL}</url>
</coverageStore>
```

**请求体格式 (JSON):**
```json
{
  "coverageStore": {
    "name": "{storeName}",
    "type": "GeoTIFF",
    "url": "{fileURL}"
  }
}
```

**Content-Type:** `application/xml` 或 `application/json`

**重要参数:**
- `url` - 外部文件的完整 URL（**不是** `file` 参数）

### MinIO HTTP URL 格式

对于存储在 MinIO 中的 GeoTIFF 文件，HTTP 访问 URL 格式为：
```
http://{minio-host}:{port}/{bucket}/{object-key}
```

示例：
- MinIO endpoint: `http://localhost:9000`
- Bucket: `gis-raster`
- Object key: `images/abc-123.tif`
- 完整 URL: `http://localhost:9000/gis-raster/images/abc-123.tif`

## 完整修正代码

### 1. GeoServerProperties.java - 添加 MinIO 配置

```java
package com.gisplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "geoserver")
@Data
public class GeoServerProperties {

    private String url = "http://localhost:8080/geoserver";
    private String username = "admin";
    private String password = "geoserver";
    private String workspace = "gisplatform";
    private int tilingMinZoom = 0;
    private int tilingMaxZoom = 18;

    // MinIO 配置 - 用于构建 HTTP URL
    private String minioUrl = "http://localhost:9000";
    private String minioBucket = "gis-raster";
}
```

### 2. GeoServerClient.java - 改进错误处理

```java
package com.gisplatform.service.geoserver;

import com.gisplatform.config.GeoServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Component
public class GeoServerClient {

    @Autowired
    private GeoServerProperties props;

    private final RestTemplate restTemplate;

    public GeoServerClient() {
        this.restTemplate = new RestTemplate();
    }

    public <T> T exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
        String url = props.getUrl() + path;
        log.debug("GeoServer API request: {} {}", method, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        String auth = props.getUsername() + ":" + props.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<Object> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
            log.debug("GeoServer API response: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("GeoServer error ({}): {}", e.getStatusCode(), errorBody);
            // 尝试从 XML 错误响应中提取 message
            String geoServerMsg = extractGeoServerErrorMessage(errorBody);
            throw new RuntimeException("GeoServer API failed: " + url +
                ", status: " + e.getStatusCode() +
                ", GeoServer error: " + geoServerMsg, e);
        } catch (Exception e) {
            throw new RuntimeException("GeoServer API call failed: " + url + ", error: " + e.getMessage(), e);
        }
    }

    public <T> T get(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.GET, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(path, HttpMethod.POST, body, responseType);
    }

    public <T> T delete(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.DELETE, null, responseType);
    }

    private String extractGeoServerErrorMessage(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.isEmpty()) {
            return "no details";
        }
        // 从 GeoServer XML 错误中提取 <message>...</message>
        try {
            int msgStart = xmlResponse.indexOf("<message>");
            int msgEnd = xmlResponse.indexOf("</message>");
            if (msgStart >= 0 && msgEnd > msgStart) {
                return xmlResponse.substring(msgStart + 9, msgEnd);
            }
        } catch (Exception ignored) {}
        // 尝试 JSON
        try {
            int msgStart = xmlResponse.indexOf("\"message\":\"");
            int msgEnd = xmlResponse.indexOf("\"", msgStart + 11);
            if (msgStart >= 0 && msgEnd > msgStart) {
                return xmlResponse.substring(msgStart + 11, msgEnd);
            }
        } catch (Exception ignored) {}
        return xmlResponse.length() > 200 ? xmlResponse.substring(0, 200) + "..." : xmlResponse;
    }

    public boolean isReachable() {
        try {
            restTemplate.getForObject(props.getUrl() + "/rest/workspaces", String.class);
            return true;
        } catch (Exception e) {
            log.warn("GeoServer is not reachable: {}", e.getMessage());
            return false;
        }
    }
}
```

### 3. GeoServerCoverageStoreService.java - 修正 API 路径和 URL

```java
package com.gisplatform.service.geoserver;

import com.gisplatform.config.GeoServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GeoServerCoverageStoreService {

    @Autowired
    private GeoServerClient client;

    @Autowired
    private GeoServerProperties props;

    /**
     * 创建外部 GeoTIFF coverage store。
     * 使用 GeoServer 2.28.x 的 external.geotiff endpoint。
     *
     * @param workspace  工作区名称
     * @param storeName  coverage store 名称
     * @param minioBucket MinIO 存储桶名称
     * @param minioKey   MinIO 对象路径 (如 "images/uuid.tif")
     */
    public void createImageMosaicStore(String workspace, String storeName,
                                        String minioBucket, String minioKey) {
        if (storeExists(workspace, storeName)) {
            log.info("CoverageStore {} already exists, skipping", storeName);
            return;
        }

        // 构建 MinIO HTTP URL
        // 格式: http://localhost:9000/bucket/key
        String minioUrl = props.getMinioUrl();
        String fileUrl = minioUrl + "/" + minioBucket + "/" + minioKey;
        log.info("Using MinIO file URL: {}", fileUrl);

        // GeoServer 2.28.x 使用 external.geotiff endpoint
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<coverageStore>");
        xml.append("<name>").append(storeName).append("</name>");
        xml.append("<type>GeoTIFF</type>");
        xml.append("<url>").append(fileUrl).append("</url>");
        xml.append("</coverageStore>");

        String endpoint = "/rest/workspaces/" + workspace +
            "/coveragestores/" + storeName + "/external.geotiff";

        log.info("Creating GeoTIFF coverage store via: {}", endpoint);

        try {
            client.post(endpoint, xml.toString(), String.class);
            log.info("Created GeoTIFF coverage store: {} -> {}", storeName, fileUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GeoTIFF coverage store: " + storeName +
                ". File URL: " + fileUrl, e);
        }
    }

    public boolean storeExists(String workspace, String storeName) {
        try {
            ResponseEntity<String> response = new RestTemplate().exchange(
                props.getUrl() + "/rest/workspaces/" + workspace + "/coveragestores/" + storeName,
                HttpMethod.GET,
                null,
                String.class
            );
            boolean exists = response.getStatusCode().is2xxSuccessful();
            log.debug("Store {} exists: {}", storeName, exists);
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteStore(String workspace, String storeName) {
        try {
            client.delete("/rest/workspaces/" + workspace + "/coveragestores/" + storeName, String.class);
            log.info("Deleted coverage store: {}", storeName);
        } catch (Exception e) {
            log.warn("Failed to delete coverage store: {}", storeName, e);
        }
    }
}
```

### 4. ImageServiceImpl.java - 修正发布方法传递正确的 MinIO 信息

修改 `publishImageDataset` 方法，从 raster_metadata 获取 bucket 名称：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Dataset publishImageDataset(Long id) {
    Dataset dataset = this.getById(id);
    if (dataset == null || dataset.getDeleted() == 1) {
        throw new RuntimeException("数据集不存在");
    }

    if (!"raster".equals(dataset.getType())) {
        throw new RuntimeException("只有影像数据集可以发布");
    }

    if ("published".equals(dataset.getStatus())) {
        throw new RuntimeException("数据集已发布");
    }

    // 获取 raster_metadata 中的 MinIO bucket
    LambdaQueryWrapper<RasterMetadata> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(RasterMetadata::getDatasetId, id);
    RasterMetadata rasterMetadata = rasterMetadataMapper.selectOne(queryWrapper);

    if (rasterMetadata == null) {
        throw new RuntimeException("raster_metadata 不存在");
    }

    String minioBucket = rasterMetadata.getMinioBucket();
    String minioKey = rasterMetadata.getMinioKey();

    if (minioBucket == null || minioBucket.isEmpty()) {
        // 使用配置的默认值
        minioBucket = props.getMinioBucket(); // 或 rasterBucket
    }
    if (minioKey == null || minioKey.isEmpty()) {
        minioKey = dataset.getMinioKey();
    }

    String workspace = geoServerProperties.getWorkspace();
    String storeName = "raster_" + id;
    String layerName = "raster_" + id;

    // Create workspace if not exists
    workspaceService.createWorkspace(workspace);

    // Create GeoTIFF coverage store (pass bucket + key)
    coverageStoreService.createImageMosaicStore(workspace, storeName, minioBucket, minioKey);

    // Publish layer
    layerService.publishLayer(workspace, storeName, layerName, dataset.getName());

    // Set URL fields
    dataset.setWmsUrl(layerService.getWmsUrl(workspace, layerName));
    dataset.setWmtsUrl(layerService.getWmtsUrl(workspace, layerName));

    // Update status
    dataset.setStatus("published");
    dataset.setTileStatus("pending");
    dataset.setCacheSeedStatus("idle");
    dataset.setUpdateTime(LocalDateTime.now());

    this.updateById(dataset);

    // Trigger async tiling
    tileSeedService.triggerSeed(id, geoServerProperties.getTilingMinZoom(),
        geoServerProperties.getTilingMaxZoom());

    return dataset;
}
```

## application.yml 配置更新

```yaml
# GeoServer 配置
geoserver:
  url: ${GEOSERVER_URL:http://localhost:8080/geoserver}
  username: ${GEOSERVER_USERNAME:admin}
  password: ${GEOSERVER_PASSWORD:geoserver}  # 确保有默认值
  workspace: ${GEOSERVER_WORKSPACE:gisplatform}
  # MinIO HTTP 访问地址 (GeoServer 需要通过 HTTP 访问)
  minio-url: ${MINIO_ENDPOINT:http://localhost:9000}
  minio-bucket: ${MINIO_BUCKET_RASTER:gis-raster}
  tiling-min-zoom: 0
  tiling-max-zoom: 18
```

## 调试建议

### 1. 直接测试 GeoServer API

```bash
# 检查 workspace 是否存在
curl -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/gisplatform

# 测试创建 coverage store (用实际可访问的文件 URL)
curl -u admin:geoserver -X POST \
  -H "Content-Type: application/xml" \
  -d '<coverageStore><name>test_geotiff</name><type>GeoTIFF</type><url>file:///tmp/test.tif</url></coverageStore>' \
  http://localhost:8080/geoserver/rest/workspaces/gisplatform/coveragestores/test_geotiff/external.geotiff

# 检查 coverage store 是否创建成功
curl -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/gisplatform/coveragestores/test_geotiff
```

### 2. 验证 MinIO 文件可访问

确保 GeoServer 服务器能够通过 HTTP 访问 MinIO：
```bash
# 在 GeoServer 服务器上测试
curl -I http://localhost:9000/gis-raster/images/uuid.tif
```

### 3. 查看 GeoServer 日志

```bash
# GeoServer 日志位置
cat /var/geoserver/logs/geoserver.log
# 或 UI: Server Status → Logs
```

## 关键修正点总结

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    修正前后对比                                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  错误                              正确                                  │
│  ─────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  POST /coveragestores          →  POST /coveragestores/{name}/          │
│                                                    external.geotiff     │
│                                                                          │
│  s3://minio/bucket/key         →  http://minio:9000/bucket/key         │
│                                                                          │
│  coverageStoreService.         →  coverageStoreService.                 │
│    createImageMosaicStore(       createImageMosaicStore(                 │
│      ws, store, minioKey)          ws, store, minioBucket, minioKey)    │
│                                                                          │
│  e.getMessage()                →  从 XML 响应提取 <message>             │
│                                                                          │
│  password: ${GEOSERVER_        →  password: ${GEOSERVER_PASSWORD:        │
│    PASSWORD:}                     geoserver}                            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 注意事项

1. **external.geotiff vs external.imageMosaic**: 对于单个 GeoTIFF 文件，使用 `external.geotiff`；只有需要处理多时相或镶嵌数据集时才用 `external.imageMosaic`

2. **GeoServer 必须能访问 MinIO URL**: 如果 GeoServer 和 MinIO 不在同一网络，需要配置 MinIO 为公开访问或使用 presigned URL

3. **S3 协议替代方案**: 如果必须使用 S3 协议，需要在 GeoServer 中安装并配置 S3 插件，配置 AWS 凭证
