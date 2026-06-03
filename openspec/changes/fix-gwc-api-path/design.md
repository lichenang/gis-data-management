# 设计: fix-gwc-api-path

## 当前问题

`GeoServerCacheService.java` 中 `getSeedStatus` 方法：

```java
// Line 59 - 错误的 URL
String url = "/rest/gwc/layers/" + layerId + "/seeds.json";

// Line 66 - 错误的 JSON 解析
JsonNode runs = root.get("runs");
```

### 期望的 JSON 格式（错误）
```json
{
  "runs": [
    {
      "taskId": 1,
      "status": "RUNNING",
      "tilesTotal": 1000,
      "tilesCached": 450,
      "progress": 45.0
    }
  ]
}
```

### GeoServer GWC 实际返回的格式（正确）
```json
{
  "long": {
    "taskId": 1,
    "status": "RUNNING",
    "tilesTotal": 1000,
    "tilesCached": 450,
    "progress": 45.0
  }
}
```

## 修改方案

### 1. 修正 URL 路径

```java
// 修改前
String url = "/rest/gwc/layers/" + layerId + "/seeds.json";

// 修改后
String url = "/rest/gwc/layers/" + layerId + "/seed.json";
```

### 2. 修正 JSON 解析逻辑

```java
// 修改前
JsonNode runs = root.get("runs");
if (runs != null && runs.isArray() && runs.size() > 0) {
    JsonNode latestRun = runs.get(0);
    result.put("status", latestRun.has("status") ? latestRun.get("status").asText() : "UNKNOWN");
    result.put("tilesTotal", latestRun.has("tilesTotal") ? latestRun.get("tilesTotal").asLong() : 0L);
    result.put("tilesCached", latestRun.has("tilesCached") ? latestRun.get("tilesCached").asLong() : 0L);
    result.put("progress", latestRun.has("progress") ? latestRun.get("progress").asInt() : 0);
}

// 修改后
JsonNode longNode = root.get("long");
if (longNode != null) {
    result.put("status", longNode.has("status") ? longNode.get("status").asText() : "UNKNOWN");
    result.put("tilesTotal", longNode.has("tilesTotal") ? longNode.get("tilesTotal").asLong() : 0L);
    result.put("tilesCached", longNode.has("tilesCached") ? longNode.get("tilesCached").asLong() : 0L);
    result.put("progress", longNode.has("progress") ? longNode.get("progress").asInt() : 0);
}
```

## 验证步骤

1. 启动 GeoServer 并发布一个影像图层
2. 调用 `/api/v1/images/{id}/tiling-status` 接口
3. 确认返回的 progress、tilesTotal、tilesCached 数值正确
4. 确认日志中没有 404 错误
