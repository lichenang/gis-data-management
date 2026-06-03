## Context

根据 `openspec/specs/debug-gwc-api-call.md` 的诊断报告，GeoServerCacheService.seedLayer() 方法发送的 GWC REST API 请求格式不正确，导致切片任务从未真正启动。

## Goals / Non-Goals

**Goals:**
- 修复 GWC API 请求体格式（form-urlencoded → JSON）
- 修复异常处理（静默吞掉 → 抛出异常）
- 确保调用方能感知切片任务启动失败

**Non-Goals:**
- 不修改 GeoServerClient 的通用逻辑
- 不添加新的 API 端点兼容性逻辑（除非当前端点确实不可用）

## Implementation Details

### 修改 seedLayer 方法

```java
public void seedLayer(String workspace, String layerName, int minZoom, int maxZoom) {
    String layerId = workspace + ":" + layerName;

    // 构建 JSON 请求体
    Map<String, Object> seedRequest = new HashMap<>();
    Map<String, Object> bounds = new HashMap<>();
    Map<String, Double> coords = new HashMap<>();
    coords.put("minX", -180.0);
    coords.put("minY", -90.0);
    coords.put("maxX", 180.0);
    coords.put("maxY", 90.0);
    bounds.put("coords", coords);

    seedRequest.put("name", layerId);
    seedRequest.put("zoomStart", minZoom);
    seedRequest.put("zoomStop", maxZoom);
    seedRequest.put("format", "image/png");
    seedRequest.put("bounds", bounds);
    seedRequest.put("threadCount", 4);

    Map<String, Object> body = new HashMap<>();
    body.put("seedRequest", seedRequest);

    try {
        client.exchangeWithContentType(
            "/gwc/rest/seed/" + layerId,
            HttpMethod.POST,
            body,
            String.class,
            MediaType.APPLICATION_JSON  // ✅ 改为 JSON
        );
        log.info("Started seed task for layer: {} (zoom levels {}-{})", layerId, minZoom, maxZoom);
    } catch (Exception e) {
        log.error("Failed to start seed task for layer: {}", layerId, e);
        throw new RuntimeException("Failed to start GWC seed task for layer: " + layerId, e);
    }
}
```

### 关键变更点

1. **Content-Type**: `application/x-www-form-urlencoded` → `application/json`
2. **请求体**: 从字符串拼接改为 Map 结构
3. **异常处理**: 从 log.warn 改为 log.error + throw RuntimeException

## Verification

1. Maven 编译通过
2. 发布影像后检查日志是否显示 "Started seed task for layer..."
3. 检查 GWC 是否真正创建了种子任务（通过 GET /gwc/rest/seed/{layerId}.json）

## Migration Plan

1. 修改 GeoServerCacheService.seedLayer() 方法
2. 编译验证
3. 发布测试影像，验证自动切片是否生效

无需数据库变更，回滚只需撤销代码改动。

