## Context

当前 TileSeedService.triggerWmsSeeding() 方法使用固定的 (0,0) 瓦片坐标计算 BBOX：

```java
private int[] getRepresentativeTileCoords(int zoom) {
    return new int[]{0, 0};  // 全球范围的左上角
}

private String getTileBounds(int x, int y, int zoom) {
    // 计算出的 BBOX 是全球范围
}
```

这导致 WMS 请求使用全球范围，而影像只覆盖西安局部区域，GeoServer 拒绝处理。

## Goals / Non-Goals

**Goals:**
- 从影像元数据获取实际覆盖范围
- 将范围转换为 EPSG:3857
- 计算覆盖范围内的瓦片坐标
- 使用影像范围构建 WMS 请求

**Non-Goals:**
- 不修改数据库表结构
- 不修改其他服务

## Implementation Details

### 1. 获取影像范围

从 Dataset 的 extent 字段读取（JSON 格式）：

```java
private double[] getImageExtent(Long datasetId) {
    Dataset dataset = datasetService.getById(datasetId);
    String extentJson = dataset.getExtent();

    if (extentJson == null || extentJson.isEmpty()) {
        return null;
    }

    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> extent = mapper.readValue(extentJson, Map.class);
        double minX = ((Number) extent.get("minX")).doubleValue();
        double minY = ((Number) extent.get("minY")).doubleValue();
        double maxX = ((Number) extent.get("maxX")).doubleValue();
        double maxY = ((Number) extent.get("maxY")).doubleValue();
        return new double[]{minX, minY, maxX, maxY};
    } catch (Exception e) {
        log.warn("Failed to parse extent for dataset {}: {}", datasetId, e.getMessage());
        return null;
    }
}
```

### 2. 坐标转换 EPSG:4326 → EPSG:3857

```java
private double[] convertToWebMercator(double minX, double minY, double maxX, double maxY) {
    // 使用 Web Mercator 公式转换
    double mercatorMinX = minX * 20037508.34 / 180;
    double mercatorMaxX = maxX * 20037508.34 / 180;
    double mercatorMinY = Math.log(Math.tan((90 + minY) * Math.PI / 360)) / Math.PI / 2 * 20037508.34;
    double mercatorMaxY = Math.log(Math.tan((90 + maxY) * Math.PI / 360)) / Math.PI / 2 * 20037508.34;
    return new double[]{mercatorMinX, mercatorMinY, mercatorMaxX, mercatorMaxY};
}
```

### 3. 计算覆盖范围内的瓦片坐标

```java
private List<int[]> getTileCoordsInExtent(double minX, double minY, double maxX, double maxY, int zoom) {
    List<int[]> coords = new ArrayList<>();

    int minTileX = (int) Math.floor((minX + 20037508.34) / (2 * 20037508.34 / (1 << zoom)));
    int maxTileX = (int) Math.floor((maxX + 20037508.34) / (2 * 20037508.34 / (1 << zoom)));
    int minTileY = (int) Math.floor((20037508.34 - maxY) / (2 * 20037508.34 / (1 << zoom)));
    int maxTileY = (int) Math.floor((20037508.34 - minY) / (2 * 20037508.34 / (1 << zoom)));

    // 限制每个级别的瓦片数量，避免请求过多
    int maxTilesPerZoom = 10;
    int count = 0;

    for (int x = minTileX; x <= maxTileX && count < maxTilesPerZoom; x++) {
        for (int y = minTileY; y <= maxTileY && count < maxTilesPerZoom; y++) {
            coords.add(new int[]{x, y});
            count++;
        }
    }

    return coords;
}
```

### 4. 修改 triggerWmsSeeding 方法

```java
private void triggerWmsSeeding(Long datasetId, Integer minZoom, Integer maxZoom) {
    String layerName = props.getWorkspace() + ":raster_" + datasetId;
    String wmsUrl = props.getUrl() + "/wms";

    // 获取影像的实际范围 (EPSG:4326)
    double[] extent4326 = getImageExtent(datasetId);
    if (extent4326 == null) {
        log.warn("No extent found for dataset {}, using fallback", datasetId);
        // 回退到原来的逻辑
        triggerWmsSeedingFallback(datasetId, minZoom, maxZoom);
        return;
    }

    // 转换为 EPSG:3857
    double[] extent3857 = convertToWebMercator(
        extent4326[0], extent4326[1], extent4326[2], extent4326[3]
    );

    log.info("Using image extent for dataset {}: {} (EPSG:3857)",
        datasetId, Arrays.toString(extent3857));

    // 对每个 zoom 级别发送覆盖范围内的 WMS 请求
    for (int z = minZoom; z <= maxZoom; z++) {
        List<int[]> tileCoords = getTileCoordsInExtent(
            extent3857[0], extent3857[1], extent3857[2], extent3857[3], z
        );

        for (int[] coords : tileCoords) {
            int x = coords[0];
            int y = coords[1];
            String bbox = getTileBounds(x, y, z);

            String requestUrl = String.format(
                "%s?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=%s&FORMAT=image/png&WIDTH=256&HEIGHT=256&CRS=EPSG:3857&BBOX=%s",
                wmsUrl, layerName, bbox
            );

            try {
                restTemplate.getForObject(requestUrl, String.class);
                log.debug("Sent WMS request for layer {} at tile {}/{}", layerName, z, x, y);
            } catch (Exception e) {
                log.warn("WMS request failed: {}", e.getMessage());
            }
        }
    }
}
```

### 5. 回退逻辑

如果无法获取影像范围，使用原来的逻辑：

```java
private void triggerWmsSeedingFallback(Long datasetId, Integer minZoom, Integer maxZoom) {
    // 原有逻辑，保持向后兼容
}
```

## Verification

1. Maven 编译通过
2. 发布一个西安区域的影像
3. 检查日志是否显示正确的影像范围
4. 检查 GeoServer 是否成功生成切片

