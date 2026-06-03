## Context

GeoWebCache REST API 是官方推荐的种子任务触发方式，比 WMS 请求更可靠，直接操作 GWC 内部状态。

## GWC REST API 规范

**端点**: `POST /gwc/rest/seed/{layerId}`

**Content-Type**: `application/x-www-form-urlencoded`

**参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| name | string | 图层ID，格式：workspace:layerName |
| zoomStart | string | 起始缩放级别 |
| zoomStop | string | 结束缩放级别 |
| format | string | 切片格式，如 image/png |
| bounds | string | 地理范围，格式：minX,minY,maxX,maxY |
| threadCount | string | 并发线程数 |
| type | string | 任务类型：seed/reseed/truncate |

**调用示例**:
```
POST /geoserver/gwc/rest/seed/gisplatform:raster_40
Content-Type: application/x-www-form-urlencoded

name=gisplatform:raster_40&zoomStart=0&zoomStop=14&format=image/png&bounds=12129263.4322,4050247.7206,12130944.1474,4051356.3316&threadCount=4&type=seed
```

## Implementation

### 修改 TileSeedService.triggerSeed()

```java
@Async("tilingExecutor")
public void triggerSeed(Long datasetId, Integer minZoom, Integer maxZoom) {
    log.info("Starting GWC seed task for dataset {} (zoom {}-{})", datasetId, minZoom, maxZoom);

    Dataset dataset = datasetService.getById(datasetId);
    if (dataset == null) {
        log.error("Dataset {} not found", datasetId);
        return;
    }

    String jobId = UUID.randomUUID().toString();
    dataset.setTileJobId(jobId);
    dataset.setTileStatus("processing");
    dataset.setTileProgress(0);
    datasetService.updateById(dataset);

    try {
        triggerGwcSeedTask(datasetId, minZoom, maxZoom);

        // 轮询种子任务状态
        String layerName = "raster_" + datasetId;
        pollSeedStatus(datasetId, layerName);

    } catch (Exception e) {
        log.error("Tile seed failed for dataset {}", datasetId, e);
        dataset.setTileStatus("failed");
        dataset.setCacheSeedStatus("idle");
        dataset.setTileProgress(0);
        datasetService.updateById(dataset);
    }
}

private void triggerGwcSeedTask(Long datasetId, Integer minZoom, Integer maxZoom) {
    String layerId = props.getWorkspace() + ":raster_" + datasetId;
    String gwcUrl = props.getUrl() + "/gwc/rest/seed/" + layerId;

    // 获取影像范围作为 bounds
    String bounds = getImageExtentBounds(datasetId);
    if (bounds == null) {
        bounds = "-180,-90,180,90"; // 默认全球范围
    }

    // 构建 form-urlencoded 请求体
    String params = String.format(
        "name=%s&zoomStart=%d&zoomStop=%d&format=image/png&bounds=%s&threadCount=4&type=seed",
        layerId, minZoom, maxZoom, bounds
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // 添加 Basic Auth
    String auth = props.getUsername() + ":" + props.getPassword();
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    headers.set("Authorization", "Basic " + encodedAuth);

    HttpEntity<String> request = new HttpEntity<>(params, headers);

    try {
        restTemplate.exchange(
            gwcUrl,
            HttpMethod.POST,
            request,
            String.class
        );
        log.info("GWC seed task started for layer: {} (zoom {}-{}, bounds: {})", 
            layerId, minZoom, maxZoom, bounds);
    } catch (Exception e) {
        log.error("Failed to start GWC seed task for layer: {}", layerId, e);
        throw new RuntimeException("Failed to start GWC seed task", e);
    }
}

private String getImageExtentBounds(Long datasetId) {
    Dataset dataset = datasetService.getById(datasetId);
    if (dataset == null || dataset.getExtent() == null) {
        return null;
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> extent = mapper.readValue(dataset.getExtent(), Map.class);

        double minX = ((Number) extent.get("minX")).doubleValue();
        double minY = ((Number) extent.get("minY")).doubleValue();
        double maxX = ((Number) extent.get("maxX")).doubleValue();
        double maxY = ((Number) extent.get("maxY")).doubleValue();

        return String.format("%f,%f,%f,%f", minX, minY, maxX, maxY);
    } catch (Exception e) {
        log.warn("Failed to parse extent for dataset {}: {}", datasetId, e.getMessage());
        return null;
    }
}
```

### 需要添加的依赖

- `MediaType` 和 `HttpMethod` 从 org.springframework.http
- `HttpHeaders`, `HttpEntity` 从 org.springframework.http
- `Base64` 从 java.util

## Verification

1. Maven 编译通过
2. 发布影像后检查日志是否显示 "GWC seed task started"
3. 通过 GWC REST API 验证种子任务状态

