# 设计: fix-gwc-rest-base-url

## 问题分析

### URL 拼接逻辑

`GeoServerClient` 使用 `props.getUrl() + path` 拼接完整 URL：
```java
// GeoServerClient.java:33
String url = props.getUrl() + path;
```

`props.getUrl()` 通常为 `http://localhost:8080/geoserver`

### 当前错误的路径

`GeoServerCacheService.java` 中 GWC API 路径：
```java
// Line 50 - seedLayer
client.post("/rest/gwc/layers/" + layerId + "/seed", ...);

// Line 59 - getSeedStatus
String url = "/rest/gwc/layers/" + layerId + "/seed.json";
```

拼接结果（错误）：
```
http://localhost:8080/geoserver/rest/gwc/layers/{layerId}/seed
```

### 正确的 GeoServer GWC REST API 路径

根据 GeoServer 官方文档，正确的路径格式为：
```
http://host:port/geoserver/gwc/rest/layers/{layerId}/seed
```

即 `gwc/rest` 而非 `rest/gwc`。

## 修改方案

### 1. seedLayer 方法

```java
// 修改前
client.post("/rest/gwc/layers/" + layerId + "/seed", xml.toString(), String.class);

// 修改后
client.post("/gwc/rest/layers/" + layerId + "/seed", xml.toString(), String.class);
```

### 2. getSeedStatus 方法

```java
// 修改前
String url = "/rest/gwc/layers/" + layerId + "/seed.json";

// 修改后
String url = "/gwc/rest/layers/" + layerId + "/seed.json";
```

## 不需要修改的部分

以下路径是正确的（WMTS 路径格式不同）：
- `getWmtsBaseUrl()` 返回 `/gwc/service/wmts` - 正确
- `getTileUrl()` 使用的路径 - 正确

## 验证步骤

1. 启动 GeoServer 并发布一个影像图层
2. 触发切片任务，检查 GeoServer 日志无 404 错误
3. 查询切片状态，确认返回正确的 JSON 数据
