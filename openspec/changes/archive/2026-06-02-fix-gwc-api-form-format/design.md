## Context

上一轮修复错误地将 GWC REST API 请求改为 JSON 格式。GeoServer 2.28.3 的 GWC SeedController.doPost 期望 form-urlencoded 格式。

GeoServer 2.28.3 GWC SeedController 源码分析：

```java
// GeoServer 2.28.3 GWC SeedController.doPost 期望的参数
String name = request.get("name");           // layer ID: workspace:layerName
String zoomStart = request.get("zoomStart"); // 起始缩放级别
String zoomStop = request.get("zoomStop");   // 结束缩放级别
String format = request.get("format");       // 切片格式: image/png
String bounds = request.get("bounds");       // 范围: minX,minY,maxX,maxY
String threadCount = request.get("threadCount"); // 线程数
String type = request.get("type");           // 任务类型: seed/reseed/truncate
```

## Goals / Non-Goals

**Goals:**
- 修正 GWC API 请求格式为正确的 form-urlencoded
- 使用 MultiValueMap 构造表单参数
- 与 GeoServer 2.28.3 SeedController 期望格式一致

**Non-Goals:**
- 不修改其他 API 端点
- 不修改 GeoServerClient 的通用逻辑

## Implementation Details

### 修改 seedLayer 方法

```java
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

public void seedLayer(String workspace, String layerName, int minZoom, int maxZoom) {
    String layerId = workspace + ":" + layerName;

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("name", layerId);
    params.add("zoomStart", String.valueOf(minZoom));
    params.add("zoomStop", String.valueOf(maxZoom));
    params.add("format", "image/png");
    params.add("bounds", "-180,-90,180,90");
    params.add("threadCount", "4");
    params.add("type", "seed");  // 明确指定任务类型

    try {
        client.exchangeWithContentType(
            "/gwc/rest/seed/" + layerId,
            HttpMethod.POST,
            params,
            String.class,
            MediaType.APPLICATION_FORM_URLENCODED
        );
        log.info("Started seed task for layer: {} (zoom levels {}-{})", layerId, minZoom, maxZoom);
    } catch (Exception e) {
        log.error("Failed to start seed task for layer: {}", layerId, e);
        throw new RuntimeException("Failed to start GWC seed task for layer: " + layerId, e);
    }
}
```

### 关键变更点

1. **Content-Type**: `application/json` → `application/x-www-form-urlencoded`
2. **请求体**: 从嵌套 Map(JSON) 改为 MultiValueMap(表单参数)
3. **移除**: 删除 seedRequest 包装和 bounds.coords 结构
4. **新增**: 添加 type=seed 参数

### 参数对照

| 参数 | 类型 | 示例值 | 说明 |
|------|------|--------|------|
| name | string | gisplatform:raster_38 | 图层 ID |
| zoomStart | string | 0 | 起始缩放级别 |
| zoomStop | string | 14 | 结束缩放级别 |
| format | string | image/png | 切片格式 |
| bounds | string | -180,-90,180,90 | 地理范围 |
| threadCount | string | 4 | 并发线程数 |
| type | string | seed | 任务类型 |

## Verification

1. Maven 编译通过
2. 发布影像后检查日志是否显示 "Started seed task for layer..."
3. 检查 GWC 是否真正创建了种子任务

## Migration Plan

1. 修改 GeoServerCacheService.seedLayer() 方法
2. 编译验证
3. 发布测试影像，验证自动切片是否生效

无需数据库变更，回滚只需撤销代码改动。

