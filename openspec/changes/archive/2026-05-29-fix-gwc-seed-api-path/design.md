# 设计: fix-gwc-seed-api-path

## 问题分析

### 当前代码中的路径（错误）

`GeoServerCacheService.java`：
```java
// seedLayer - POST
client.post("/gwc/rest/layers/" + layerId + "/seed", ...);

// getSeedStatus - GET
String url = "/gwc/rest/layers/" + layerId + "/seed.json";
```

### GeoServer GWC REST API 正确路径

根据 GeoServer 官方文档，seed 操作的正确路径格式：

| 操作 | 正确路径 |
|------|----------|
| 触发 seed | POST `/gwc/rest/seed/{layerId}` |
| 查询状态 | GET `/gwc/rest/seed/{layerId}.json` |

注意路径结构：`seed/{layerId}` 而非 `layers/{layerId}/seed`

## 修改方案

### 1. seedLayer 方法

```java
// 修改前
client.post("/gwc/rest/layers/" + layerId + "/seed", xml.toString(), String.class);

// 修改后
client.post("/gwc/rest/seed/" + layerId, xml.toString(), String.class);
```

### 2. getSeedStatus 方法

```java
// 修改前
String url = "/gwc/rest/layers/" + layerId + "/seed.json";

// 修改后
String url = "/gwc/rest/seed/" + layerId + ".json";
```

## 完整路径对比

| 方法 | 当前（错误） | 正确 |
|------|-------------|------|
| seedLayer | /gwc/rest/layers/{layer}/seed | /gwc/rest/seed/{layer} |
| getSeedStatus | /gwc/rest/layers/{layer}/seed.json | /gwc/rest/seed/{layer}.json |

## 验证步骤

1. 启动 GeoServer 并发布一个影像图层
2. 触发切片任务，检查 GeoServer 日志无 404 错误
3. 查询切片状态，确认返回正确的 JSON 数据
