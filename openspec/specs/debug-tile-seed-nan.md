# 诊断报告：TileSeedService NaN 计算问题

## 问题描述

自动切片后 GWC 缓存目录为空，日志显示 tile range 计算出现 NaN 值。

已知数据集 extent：
```json
{"maxX": 12130944.1474, "maxY": 4051356.3316, "minX": 12129263.4322, "minY": 4050247.7206}
```

## 代码分析

### 问题代码 (TileSeedService.java:140-146)

```java
private double[] convertToWebMercator(double minX, double minY, double maxX, double maxY) {
    // ❌ 错误：假设输入是 EPSG:4326 (度)，实际是 EPSG:3857 (米)
    double mercatorMinX = minX * 20037508.34 / 180;  // 12129263 * 111319 = 1350464565947 (错误的巨大值)
    double mercatorMaxX = maxX * 20037508.34 / 180;
    
    // ❌ 错误：minY=4050247 不是度数，是米。导致 (90 + 4050247) 超出范围
    double mercatorMinY = Math.log(Math.tan((90 + minY) * Math.PI / 360)) / Math.PI / 2 * 20037508.34;
    // tan(11252度) → ∞ → log(∞) → NaN
    
    double mercatorMaxY = Math.log(Math.tan((90 + maxY) * Math.PI / 360)) / Math.PI / 2 * 20037508.34;
    return new double[]{mercatorMinX, mercatorMinY, mercatorMaxX, mercatorMaxY};
}
```

### 数据来源分析

extent 数据来自 `dataset.extent` 字段，该字段在 `ImageServiceImpl.publishImageDataset()` 中设置：

```java
// ImageServiceImpl.java:276-292
String transformJson = rasterMetadata.getTransform();
if (transformJson != null && !transformJson.isEmpty()) {
    Map<String, Object> transform = mapper.readValue(transformJson, Map.class);
    Double minX = ((Number) transform.get("minX")).doubleValue();
    Double minY = ((Number) transform.get("minY")).doubleValue();
    Double maxX = ((Number) transform.get("maxX")).doubleValue();
    Double maxY = ((Number) transform.get("maxY")).doubleValue();
    String extentJson = String.format("{\"minX\":%s,\"minY\":%s,\"maxX\":%s,\"maxY\":%s}",
            minX, minY, maxX, maxY);
    dataset.setExtent(extentJson);
}
```

transform 字段来自 GeoTIFF 元数据，是**像素坐标转换矩阵**解析出的地理范围，通常已经是 EPSG:4326 或 EPSG:3857。

需要验证 raster_metadata.transform 的实际坐标系。

## 根因总结

```
┌─────────────────────────────────────────────────────────────────────┐
│                         根因链条                                      │
├─────────────────────────────────────────────────────────────────────┤
│  1. dataset.extent 存储的可能是 EPSG:3857 米制坐标                    │
│  2. getImageExtentInWebMercator() 错误地认为是 EPSG:4326             │
│  3. 调用 convertToWebMercator() 尝试再次转换                         │
│  4. 米制坐标被当作度处理，导致数值溢出或 NaN                           │
│     - X: 12129263 * 111319 = 1350464565947 (错误)                    │
│     - Y: (90 + 4050247) 超限 → tan() → ∞ → log() → NaN              │
└─────────────────────────────────────────────────────────────────────┘
```

## 修复方案

### 方案：检测输入坐标系并跳过不必要的转换

通过数值范围判断坐标系：
- EPSG:4326 (度): X 在 [-180, 180], Y 在 [-90, 90]
- EPSG:3857 (米): X 在 [-20037508, 20037508], Y 在 [-20037508, 20037508]

```java
private double[] getImageExtentInWebMercator(Long datasetId) {
    Dataset dataset = datasetService.getById(datasetId);
    if (dataset == null) {
        return null;
    }

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

        // 检测坐标系：EPSG:3857 的 Y 值通常大于 10000
        if (Math.abs(minY) > 10000 || Math.abs(maxY) > 10000) {
            // 已经是 EPSG:3857，无需转换
            log.info("Extent already in EPSG:3857, using directly");
            return new double[]{minX, minY, maxX, maxY};
        } else {
            // 是 EPSG:4326，进行转换
            return convertToWebMercator(minX, minY, maxX, maxY);
        }
    } catch (Exception e) {
        log.warn("Failed to parse extent for dataset {}: {}", datasetId, e.getMessage());
        return null;
    }
}
```

### 关键修改点

1. **getImageExtentInWebMercator()** (Lines 113-138): 添加坐标系检测逻辑
2. 如果 Y 坐标绝对值 > 10000，认为是 EPSG:3857，直接返回
3. 否则调用 convertToWebMercator() 转换

## 相关文件

- `TileSeedService.java:113-146` - extent 解析和转换逻辑

