# GWC REST API 调试分析

## 问题描述

自动切片调用 GWC REST API 返回 400 Bad Request。

当前请求：
- **URL**: `POST http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40`
- **Content-Type**: `application/x-www-form-urlencoded`
- **Body**: `name=gisplatform:raster_40&zoomStart=0&zoomStop=14&format=image/png&bounds=12129263.4322,4050247.7206,12130944.1474,4051356.3316&threadCount=4&type=seed`

---

## GeoServer 2.28.3 GWC REST API 分析

### 1. REST API 端点路径

```
/geoserver/gwc/rest/seed/{layerId}
```

- `{layerId}` 格式: `{workspace}:{layerName}`，例如 `gisplatform:raster_40`

### 2. 支持的请求格式

GWC REST API 支持多种格式：

| 格式 | Content-Type | 请求体格式 |
|------|--------------|-----------|
| form-urlencoded | application/x-www-form-urlencoded | key1=value1&key2=value2 |
| JSON | application/json | {"key1": "value1", "key2": "value2"} |
| XML | application/xml | <seedRequest>...</seedRequest> |

### 3. JSON 格式（推荐）

**推荐使用 JSON 格式**，因为更可靠且能处理复杂参数：

```http
POST http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40
Content-Type: application/json
Authorization: Basic ...
```

**请求体 (JSON)**:
```json
{
  "seedRequest": {
    "name": "gisplatform:raster_40",
    "zoomStart": "0",
    "zoomStop": "14",
    "format": "image/png",
    "bounds": "-180,-90,180,90",
    "gridSetId": "EPSG:3857",
    "threadCount": 4,
    "type": "seed"
  }
}
```

### 4. form-urlencoded 格式（备选）

如果使用 form-urlencoded，需要确保包含所有必要参数：

```
name=gisplatform:raster_40
zoomStart=0
zoomStop=14
format=image/png
bounds=-180,-90,180,90
gridSetId=EPSG:3857
threadCount=4
type=seed
```

### 5. 必要参数说明

| 参数 | 类型 | 说明 | 必填 |
|------|------|------|------|
| name | string | 图层ID，格式: workspace:layerName | ✓ |
| zoomStart | string | 起始缩放级别 | ✓ |
| zoomStop | string | 结束缩放级别 | ✓ |
| format | string | 切片格式，如 image/png | ✓ |
| bounds | string | 地理范围，格式: minX,minY,maxX,maxY | ✓ |
| gridSetId | string | 网格集ID，默认 EPSG:3857 | ✗ (可选) |
| threadCount | string | 并发线程数 | ✗ (可选) |
| type | string | 任务类型: seed/reseed/truncate | ✓ |

### 6. bounds 坐标系统问题

**关键问题**: bounds 参数使用的是哪个坐标系？

GeoWebCache 默认使用 `EPSG:3857` (Web Mercator) 或 `EPSG:4326` (经纬度)。

```
bounds 格式: minX,minY,maxX,maxY

EPSG:3857 示例: 12129263.4322,4050247.7206,12130944.1474,4051356.3316
EPSG:4326 示例: 108.5,22.8,109.0,23.0
```

**必须与 gridSetId 对应**:
- 如果 gridSetId="EPSG:3857"，bounds 必须是 Web Mercator 坐标
- 如果 gridSetId="EPSG:4326"，bounds 必须是经纬度坐标

---

## 可能导致 400 错误的原因

### 原因 1: bounds 坐标系统不匹配

如果影像的 extent 是 EPSG:4326 (经纬度)，但请求没有指定正确的 gridSetId，GWC 会拒绝请求。

### 原因 2: 缺少 gridSetId 参数

强烈建议显式指定 gridSetId 参数。

### 原因 3: layerId 不存在

确保图层已正确发布到 GWC。可以先通过以下方式验证:

```
GET http://127.0.0.1:8080/geoserver/gwc/rest/layers/gisplatform:raster_40.json
```

### 原因 4: Content-Type 不匹配

某些版本的 GWC 对 Content-Type 要求严格。尝试使用 JSON 格式。

---

## 修复方案

### 方案 A: 使用 JSON 格式（推荐）

修改 TileSeedService.java:

```java
private void triggerGwcSeedTask(Long datasetId, Integer minZoom, Integer maxZoom) {
    String layerId = props.getWorkspace() + ":raster_" + datasetId;
    String gwcUrl = props.getUrl() + "/gwc/rest/seed/" + layerId;

    String bounds = getImageExtentBounds(datasetId);
    if (bounds == null) {
        bounds = "-180,-90,180,90";
    }

    // 使用 JSON 格式
    String jsonBody = String.format(
        "{\"seedRequest\":{\"name\":\"%s\",\"zoomStart\":\"%d\",\"zoomStop\":\"%d\",\"format\":\"image/png\",\"bounds\":\"%s\",\"gridSetId\":\"EPSG:3857\",\"threadCount\":4,\"type\":\"seed\"}}",
        layerId, minZoom, maxZoom, bounds
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String auth = props.getUsername() + ":" + props.getPassword();
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    headers.set("Authorization", "Basic " + encodedAuth);

    HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

    try {
        restTemplate.exchange(gwcUrl, HttpMethod.POST, request, String.class);
        log.info("GWC seed task started for layer: {}", layerId);
    } catch (Exception e) {
        log.error("Failed to start GWC seed task for layer: {}", layerId, e);
        throw new RuntimeException("Failed to start GWC seed task", e);
    }
}
```

### 方案 B: 添加 gridSetId 到 form-urlencoded

如果继续使用 form-urlencoded，确保添加 gridSetId:

```java
String params = String.format(
    "name=%s&zoomStart=%d&zoomStop=%d&format=image/png&bounds=%s&gridSetId=EPSG:3857&threadCount=4&type=seed",
    layerId, minZoom, maxZoom, bounds
);
```

### 方案 C: 验证图层是否存在于 GWC

先检查图层是否存在:

```bash
curl -u admin:geoserver http://127.0.0.1:8080/geoserver/gwc/rest/layers/gisplatform:raster_40.json
```

如果返回 404，说明图层没有发布到 GWC。

---

## 验证步骤

1. **检查 GWC 图层是否存在**:
   ```bash
   curl -u admin:geoserver http://127.0.0.1:8080/geoserver/gwc/rest/layers/gisplatform:raster_40.json
   ```

2. **测试 seed API (form-urlencoded)**:
   ```bash
   curl -X POST \
     -u admin:geoserver \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "name=gisplatform:raster_40&zoomStart=0&zoomStop=14&format=image/png&bounds=-180,-90,180,90&gridSetId=EPSG:3857&threadCount=4&type=seed" \
     http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40
   ```

3. **测试 seed API (JSON)**:
   ```bash
   curl -X POST \
     -u admin:geoserver \
     -H "Content-Type: application/json" \
     -d '{"seedRequest":{"name":"gisplatform:raster_40","zoomStart":"0","zoomStop":"14","format":"image/png","bounds":"-180,-90,180,90","gridSetId":"EPSG:3857","threadCount":4,"type":"seed"}}' \
     http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40
   ```

4. **检查种子任务状态**:
   ```bash
   curl -u admin:geoserver http://127.0.0.1:8080/geoserver/gwc/rest/seed/gisplatform:raster_40.json
   ```

---

## 排查建议

1. 先用 curl 命令手动测试相同的请求，确认哪个格式有效
2. 在代码中添加更详细的错误日志，记录服务器返回的错误信息
3. 检查 bounds 坐标值是否合理（对于 EPSG:3857，X 应该在 -20037508.34 到 20037508.34 之间，Y 同样）
4. 确认图层已正确发布到 GWC

---

## 参考资料

- GeoWebCache REST API: https://docs.geoserver.org/main/en/user/geowebcache/rest/seed.html
- GeoServer 2.28.x Documentation
