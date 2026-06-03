# Design: add-image-publish

## 1. 数据库变更

新增字段到 `dataset` 表：

```sql
ALTER TABLE dataset ADD COLUMN tile_status VARCHAR(20) DEFAULT 'pending';
ALTER TABLE dataset ADD COLUMN tile_progress INTEGER DEFAULT 0;
ALTER TABLE dataset ADD COLUMN tile_job_id VARCHAR(64);
ALTER TABLE dataset ADD COLUMN wms_url VARCHAR(500);
ALTER TABLE dataset ADD COLUMN wmts_url VARCHAR(500);
ALTER TABLE dataset ADD COLUMN cache_seed_status VARCHAR(20) DEFAULT 'idle';
```

对应实体变更 `Dataset.java`:

```java
private String tileStatus;      // pending/processing/completed/failed
private Integer tileProgress;   // 0-100
private String tileJobId;       // UUID
private String wmsUrl;          // http://localhost:8080/geoserver/gisplatform/raster/wms
private String wmtsUrl;         // http://localhost:8080/geoserver/gisplatform/raster/wmts
private String cacheSeedStatus; // idle/seeding/seeded
```

## 2. 配置类

### AsyncConfig.java

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "tilingExecutor")
    public TaskExecutor tilingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("tiling-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### GeoServerProperties.java

```java
@ConfigurationProperties(prefix = "geoserver")
@Data
public class GeoServerProperties {
    private String url;
    private String username;
    private String password;
    private String workspace = "gisplatform";
    private int tilingMinZoom = 0;
    private int tilingMaxZoom = 18;
}
```

## 3. GeoServer REST Client

### GeoServerClient.java (基础客户端)

```java
@Component
public class GeoServerClient {

    @Autowired
    private GeoServerProperties props;

    private final RestTemplate restTemplate;

    public GeoServerClient() {
        this.restTemplate = new RestTemplate();
    }

    public <T> T exchange(String path, HttpMethod method, Object body, Class<T> responseType) {
        // 设置认证头、Content-Type
        // 执行 HTTP 请求
        // 处理响应
    }
}
```

### 服务类

| 类 | 职责 |
|-----|------|
| `GeoServerWorkspaceService` | 创建/检查工作区 `POST /rest/workspaces` |
| `GeoServerCoverageStoreService` | 创建 ImageMosaic 存储 |
| `GeoServerLayerService` | 发布图层 `POST /rest/workspaces/{ws}/layers` |
| `GeoServerCacheService` | 触发 GeoWebCache 种子任务 |

## 4. 发布流程实现

### ImageServiceImpl.publish() 核心逻辑

```java
@Transactional
public Dataset publishImageDataset(Long id) {
    // 1. 查询数据集和 raster_metadata
    Dataset dataset = getDatasetById(id);
    RasterMetadata rm = rasterMetadataMapper.selectByDatasetId(id);

    // 2. 校验状态
    if (!"draft".equals(dataset.getStatus())) {
        throw new RuntimeException("只有草稿状态可发布");
    }

    // 3. 创建 GeoServer 工作区
    workspaceService.createWorkspace(props.getWorkspace());

    // 4. 创建 ImageMosaic 存储 (指向 MinIO/PostGIS)
    String storeName = "raster_" + dataset.getId();
    coverageStoreService.createImageMosaicStore(
        props.getWorkspace(),
        storeName,
        dataset.getMinioKey(),
        props.getTilingMinZoom(),
        props.getTilingMaxZoom()
    );

    // 5. 发布图层
    String layerName = "raster_" + dataset.getId();
    layerService.publishLayer(props.getWorkspace(), storeName, layerName, dataset.getName());

    // 6. 计算 WMS/WMTS URL
    String baseUrl = props.getUrl() + "/" + props.getWorkspace();
    dataset.setWmsUrl(baseUrl + "/" + layerName + "/wms");
    dataset.setWmtsUrl(baseUrl + "/" + layerName + "/wmts");

    // 7. 更新状态
    dataset.setStatus("published");
    dataset.setTileStatus("pending");
    dataset.setCacheSeedStatus("idle");
    updateById(dataset);

    // 8. 异步触发切片任务
    tileSeedService.triggerSeed(dataset.getId(), props.getTilingMinZoom(), props.getTilingMaxZoom());

    return dataset;
}
```

### 异步切片任务

```java
@Service
@RequiredArgsConstructor
public class TileSeedService {

    @Async("tilingExecutor")
    public void triggerSeed(Long datasetId, int minZoom, int maxZoom) {
        Dataset dataset = datasetService.getById(datasetId);
        
        try {
            // 更新状态为处理中
            dataset.setTileStatus("processing");
            dataset.setCacheSeedStatus("seeding");
            datasetService.updateById(dataset);

            // 调用 GeoWebCache REST API 触发种子任务
            cacheService.seedLayer(
                props.getWorkspace(),
                "raster_" + datasetId,
                minZoom,
                maxZoom
            );

            // 轮询任务状态并更新进度
            monitorSeedProgress(datasetId);

        } catch (Exception e) {
            dataset.setTileStatus("failed");
            dataset.setCacheSeedStatus("idle");
            dataset.setTileProgress(0);
            datasetService.updateById(dataset);
            log.error("切片任务失败", e);
        }
    }

    private void monitorSeedProgress(Long datasetId) {
        // 轮询 GeoWebCache 任务状态，更新 tile_progress
        // 完成后设置 tile_status = "completed", cache_seed_status = "seeded"
    }
}
```

## 5. Controller 端点

### ImageController 扩展

```java
@PostMapping("/{id}/publish")
public R<Dataset> publish(@PathVariable Long id) {
    Dataset dataset = imageService.publishImageDataset(id);
    return R.ok(dataset);
}

@DeleteMapping("/{id}/publish")
public R<Dataset> unpublish(@PathVariable Long id) {
    Dataset dataset = imageService.unpublishImageDataset(id);
    return R.ok(dataset);
}

@PostMapping("/{id}/retile")
public R<Map<String, Object>> retile(@PathVariable Long id) {
    String jobId = imageService.triggerRetile(id);
    return R.ok(Map.of("tile_job_id", jobId, "message", "切片任务已重新提交"));
}

@GetMapping("/{id}/tiling-status")
public R<Map<String, Object>> tilingStatus(@PathVariable Long id) {
    Dataset dataset = datasetService.getById(id);
    Map<String, Object> status = Map.of(
        "status", dataset.getTileStatus(),
        "progress", dataset.getTileProgress(),
        "cache_seed_status", dataset.getCacheSeedStatus(),
        "wms_url", dataset.getWmsUrl() != null ? dataset.getWmsUrl() : "",
        "wmts_url", dataset.getWmtsUrl() != null ? dataset.getWmtsUrl() : ""
    );
    return R.ok(status);
}
```

## 6. 文件清单

| 文件 | 路径 |
|------|------|
| AsyncConfig.java | backend/src/main/java/com/gisplatform/config/AsyncConfig.java |
| GeoServerProperties.java | backend/src/main/java/com/gisplatform/config/GeoServerProperties.java |
| GeoServerClient.java | backend/src/main/java/com/gisplatform/service/geoserver/GeoServerClient.java |
| GeoServerWorkspaceService.java | backend/src/main/java/com/gisplatform/service/geoserver/GeoServerWorkspaceService.java |
| GeoServerCoverageStoreService.java | backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCoverageStoreService.java |
| GeoServerLayerService.java | backend/src/main/java/com/gisplatform/service/geoserver/GeoServerLayerService.java |
| GeoServerCacheService.java | backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java |
| TileSeedService.java | backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java |
| ImageService.java (扩展) | (已有，添加方法签名) |
| ImageController.java (扩展) | (已有，添加端点) |
| Dataset.java (扩展) | (已有，添加字段) |

## 7. 异常处理

| 场景 | HTTP 响应 |
|------|----------|
| 数据集不存在 | 404 |
| 非草稿状态发布 | 400 |
| GeoServer 连接失败 | 503 |
| ImageMosaic 创建失败 | 500 (回滚) |
| MinIO 文件不存在 | 404 |
| 切片任务失败 | 200 (返回 failed 状态，前端可重试) |
