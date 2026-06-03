# 诊断报告：GeoServer GWC REST API 调用分析

## 问题描述

影像发布后自动切片未生效。调用链路存在，但 GWC 种子任务未真正启动。

## 代码分析

### 当前实现 (`GeoServerCacheService.seedLayer`)

```java
public void seedLayer(String workspace, String layerName, int minZoom, int maxZoom) {
    String layerId = workspace + ":" + layerName;  // e.g., "gisplatform:raster_38"

    StringBuilder params = new StringBuilder();
    params.append("name=").append(layerId);
    params.append("&zoomStart=").append(minZoom);
    params.append("&zoomStop=").append(maxZoom);
    params.append("&format=image/png");
    params.append("&bounds=-180,-90,180,90");
    params.append("&threadCount=4");

    try {
        client.exchangeWithContentType(
            "/gwc/rest/seed/" + layerId,      // POST /gwc/rest/seed/gisplatform:raster_38
            HttpMethod.POST,
            params.toString(),                 // form-urlencoded string
            String.class,
            MediaType.APPLICATION_FORM_URLENCODED  // ❌ CONTENT-TYPE
        );
    } catch (Exception e) {
        log.warn("Failed to start seed task...", e);  // ⚠️ 异常被静默吞掉
    }
}
```

## 根因分析

### 根因 1: 请求体格式错误

```
┌─────────────────────────────────────────────────────────────────────┐
│  当前实现                                                            │
├─────────────────────────────────────────────────────────────────────┤
│  Method:       POST                                                 │
│  URL:          /gwc/rest/seed/{layerId}                            │
│  Content-Type: application/x-www-form-urlencoded                   │
│  Body:         name=ws:layer&zoomStart=0&zoomStop=14...            │
├─────────────────────────────────────────────────────────────────────┤
│  GeoServer GWC 期望 (GeoServer 2.28.x)                              │
├─────────────────────────────────────────────────────────────────────┤
│  Method:       POST                                                 │
│  URL:          /gwc/rest/seed/{layerId}  或  /rest/gwc/layers/{layer}/seed │
│  Content-Type: application/json  或  application/xml               │
│  Body (JSON):  {                                                    │
│                  "seedRequest": {                                  │
│                    "name": "workspace:layerName",                  │
│                    "zoomStart": 0,                                 │
│                    "zoomStop": 14,                                 │
│                    "format": "image/png",                          │
│                    "bounds": {                                     │
│                      "coords": {"minX": -180, "minY": -90,        │
│                                 "maxX": 180, "maxY": 90}           │
│                    },                                               │
│                    "threadCount": 4                                │
│                  }                                                  │
│                }                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

GeoServer GWC REST API **不支持 form-urlencoded 格式**，需要 JSON 或 XML。

### 根因 2: 异常被静默吞掉

```java
} catch (Exception e) {
    log.warn("Failed to start seed task for layer: {}, continuing without caching", layerId, e);
    // ⚠️ 只打印 warn，调用方不知道失败了
}
```

即使 API 调用失败，也只是打印警告，不抛出异常，导致：
1. 调用者以为成功了
2. 没有任何错误提示给用户
3. 切片状态永远是 "pending"

### 根因 3: API 端点可能不正确

有两个可能的 GWC 种子任务端点：

| 端点 | 说明 |
|------|------|
| `/gwc/rest/seed/{layerId}` | 传统 GWC REST 端点 |
| `/rest/gwc/layers/{layer}/seed` | 新版 REST 端点 (GeoServer 2.17+) |

当前代码使用 `/gwc/rest/seed/...` 可能与 GeoServer 2.28.3 不兼容。

## API 规范参考

### GeoServer GWC Seed API (官方)

**端点**: `POST /gwc/rest/seed/{layerId}`

**Content-Type**: `application/json`

**请求体 (JSON)**:
```json
{
  "seedRequest": {
    "name": "workspace:layername",
    "zoomStart": 0,
    "zoomStop": 14,
    "format": "image/png",
    "bounds": {
      "coords": {
        "minX": -180.0,
        "minY": -90.0,
        "maxX": 180.0,
        "maxY": 90.0
      }
    },
    "threadCount": 4
  }
}
```

**响应**:
- 成功 (200): 返回任务状态 XML/JSON
- 错误 (4xx/5xx): 返回错误信息

### 正确的调用方式

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
            body,                              // 传入 Map 会自动序列化为 JSON
            String.class,
            MediaType.APPLICATION_JSON         // ✅ 正确的内容类型
        );
    } catch (Exception e) {
        log.error("Failed to start seed task for layer: {}", layerId, e);
        throw e;  // ✅ 抛出异常，不要静默吞掉
    }
}
```

## 修复方案

### 方案 1: 修改 GeoServerCacheService (推荐)

1. 改变请求体格式为 JSON
2. 不要静默吞掉异常

### 方案 2: 使用正确的 API 端点

尝试 `/rest/gwc/layers/{layerName}/seed` 端点（GeoServer 2.17+ 推荐）

## 待验证

- [ ] GeoServer 2.28.3 实际使用的 GWC REST API 版本
- [ ] GWC 是否已启用 REST API (需要在 GeoServer web.xml 中启用)
- [ ] 认证是否正确配置 (当前使用 Basic Auth)

## 相关文件

- `GeoServerCacheService.java:37-59` - seedLayer 方法
- `GeoServerClient.java:32-57` - exchangeWithContentType 方法
- `TileSeedService.java:44-48` - triggerSeed 调用

