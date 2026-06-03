## Context

GeoServer GWC REST API 多次修复尝试均失败：
- form-urlencoded: 400 Bad Request
- JSON: 也不被正确处理
- GWC 缓存目录创建失败

GeoServer 内置机制：通过 WMS GetMap 请求可触发 GWC 自动切片。GWC 拦截 WMS 请求，检查缓存是否存在，如不存在则触发后台切片生成。

## Goals / Non-Goals

**Goals:**
- 使用 WMS GetMap 请求触发 GWC 自动切片
- 保持异步执行，不阻塞发布流程
- 覆盖多个 zoom 级别（0-18）

**Non-Goals:**
- 不依赖 GWC REST API
- 不修改 GeoServer 配置

## Implementation Details

### 修改 TileSeedService.triggerSeed()

```java
@Async("tilingExecutor")
public void triggerSeed(Long datasetId, Integer minZoom, Integer maxZoom) {
    log.info("Starting WMS-based tile seed for dataset {} (zoom {}-{})", datasetId, minZoom, maxZoom);

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
        // 使用 WMS 请求触发切片
        triggerWmsSeeding(datasetId, minZoom, maxZoom);

        // 等待并轮询状态
        pollSeedStatus(datasetId, "raster_" + datasetId);

    } catch (Exception e) {
        log.error("Tile seed failed for dataset {}", datasetId, e);
        dataset.setTileStatus("failed");
        dataset.setCacheSeedStatus("idle");
        dataset.setTileProgress(0);
        datasetService.updateById(dataset);
    }
}

private void triggerWmsSeeding(Long datasetId, Integer minZoom, Integer maxZoom) {
    String layerName = "gisplatform:raster_" + datasetId;
    String wmsUrl = geoServerProperties.getUrl() + "/wms";

    // 对每个 zoom 级别发送少量代表性瓦片请求
    for (int z = minZoom; z <= maxZoom; z++) {
        // 计算代表性瓦片坐标 (使用固定坐标，每个级别一个请求即可触发)
        int[] tileCoords = getRepresentativeTileCoords(z);

        for (int[] coords : tileCoords) {
            int x = coords[0];
            int y = coords[1];

            String requestUrl = String.format(
                "%s?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=%s&FORMAT=image/png&WIDTH=256&HEIGHT=256&CRS=EPSG:3857&BBOX=%f,%f,%f,%f",
                wmsUrl, layerName,
                getTileBounds(x, y, z)
            );

            try {
                restTemplate.getForObject(requestUrl, String.class);
                log.debug("Sent WMS request for layer {} at zoom {}", layerName, z);
            } catch (Exception e) {
                log.warn("WMS request failed for layer {} at zoom {}: {}", layerName, z, e.getMessage());
            }
        }
    }

    log.info("Sent WMS seed requests for dataset {} (zoom {}-{})", datasetId, minZoom, maxZoom);
}

// 计算代表性瓦片坐标（每个级别取左上角代表性瓦片）
private int[] getRepresentativeTileCoords(int zoom) {
    // 对每个 zoom 级别，取 0,0 瓦片（或其他代表性坐标）
    return new int[]{0, 0};
}

// 计算瓦片边界（EPSG:3857）
private String getTileBounds(int x, int y, int zoom) {
    int n = 1 << zoom;
    double minX = -20037508.34 + (x * 2 * 20037508.34 / n);
    double maxX = -20037508.34 + ((x + 1) * 2 * 20037508.34 / n);
    double minY = 20037508.34 - ((y + 1) * 2 * 20037508.34 / n);
    double maxY = 20037508.34 - (y * 2 * 20037508.34 / n);
    return String.format("%f,%f,%f,%f", minX, minY, maxX, maxY);
}
```

### 依赖注入

```java
@Autowired
private GeoServerProperties geoServerProperties;

@Autowired
private RestTemplate restTemplate;  // 需要新增或使用现有的
```

### RestTemplate 配置

可以复用现有的 RestTemplate 或创建新实例：

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

## WMS 触发切片的原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    WMS 触发 GWC 切片流程                         │
└─────────────────────────────────────────────────────────────────┘

  WMS GetMap 请求
         │
         ▼
  ┌─────────────┐
  │   GWC       │
  │ Interceptor │
  └──────┬──────┘
         │
         ▼ 检查缓存是否存在
    ┌────┴────┐
    │ 缓存    │
    │ 不存在  │
    └────┬────┘
         │
         ▼ 后台触发切片生成
  ┌─────────────┐
  │ GeoServer   │
  │ Tile Worker │
  └─────────────┘
         │
         ▼ 返回 WMS 响应
    瓦片 + 缓存
```

## Verification

1. Maven 编译通过
2. 发布影像后检查日志
3. 检查 GeoServer 数据目录中 gisplatform_raster_{id} 是否生成了切片文件

## Migration Plan

1. 修改 TileSeedService.triggerSeed() 方法
2. 添加 WMS 触发逻辑
3. 移除对 GeoServerCacheService.seedLayer() 的依赖
4. 编译验证
5. 发布测试影像，验证自动切片是否生效

无需数据库变更，回滚只需撤销代码改动。

