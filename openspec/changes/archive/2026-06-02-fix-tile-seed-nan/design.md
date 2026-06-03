## Context

根据 `openspec/specs/debug-tile-seed-nan.md` 的诊断，dataset.extent 已经是 EPSG:3857 米制坐标，但代码错误地再次调用 convertToWebMercator() 导致 NaN。

## Implementation Details

修改 `getImageExtentInWebMercator()` 方法，添加坐标系检测：

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

        // 检测坐标系：EPSG:3857 的 Y 值绝对值大于 10000
        if (Math.abs(minY) > 10000 || Math.abs(maxY) > 10000) {
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

关键点：
- Y 坐标绝对值 > 10000 → 已是 EPSG:3857 米制 → 直接返回
- Y 坐标绝对值 ≤ 10000 → 可能是 EPSG:4326 度数 → 调用转换方法

