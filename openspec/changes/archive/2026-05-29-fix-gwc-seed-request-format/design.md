# 设计: fix-gwc-seed-request-format

## 问题分析

### 当前代码（GeoServerCacheService.java seedLayer 方法）

```java
StringBuilder xml = new StringBuilder();
xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
xml.append("<seed>");
xml.append("<name>").append(layerId).append("</name>");
xml.append("<zoomStart>").append(minZoom).append("</zoomStart>");
xml.append("<zoomStop>").append(maxZoom).append("</zoomStop>");
xml.append("<format>image/png</format>");
xml.append("<bounds>-180,-90,180,90</bounds>");
xml.append("<threadCount>4</threadCount>");
xml.append("</seed>");

client.post("/gwc/rest/seed/" + layerId, xml.toString(), String.class);
```

`GeoServerClient` 默认使用 `MediaType.APPLICATION_XML`（见 `exchange` 方法）。

### GeoServer 2.28.3 GWC 期望的格式

GWC SeedController 期望 `application/x-www-form-urlencoded` 格式，参数为：
```
name={layerId}&zoomStart={minZoom}&zoomStop={maxZoom}&format=image/png&bounds=-180,-90,180,90&threadCount=4
```

## 修改方案

### 1. 创建 URL 编码格式的请求体

```java
// 构建 URL 编码格式的请求体
StringBuilder params = new StringBuilder();
params.append("name=").append(layerId);
params.append("&zoomStart=").append(minZoom);
params.append("&zoomStop=").append(maxZoom);
params.append("&format=image/png");
params.append("&bounds=-180,-90,180,90");
params.append("&threadCount=4");

client.post("/gwc/rest/seed/" + layerId, params.toString(), String.class);
```

### 2. 修改 GeoServerClient 以支持 form-urlencoded

需要使用 `exchangeWithContentType` 方法，并指定 `MediaType.APPLICATION_FORM_URLENCODED`：

```java
client.exchangeWithContentType(
    "/gwc/rest/seed/" + layerId,
    HttpMethod.POST,
    params.toString(),
    String.class,
    MediaType.APPLICATION_FORM_URLENCODED
);
```

## 验证步骤

1. 启动 GeoServer 并发布一个影像图层
2. 触发切片任务，检查 GeoServer 日志无解析错误
3. 确认切片任务成功启动
