# Proposal: fix-minio-url-localhost

## 问题描述

影像发布流程中，后端通过 PUT `external.geotiff?configure=first` 将 MinIO 文件 URL 传给 GeoServer。
当前构造的 URL 为：

```
http://minio:9000/gis-raster/images/uuid.tif
```

其中 `minio` 是 Docker Compose 网络内部的容器主机名。GeoServer 运行在独立的进程/容器中，
**无法解析 `minio` 这个主机名**，导致 Step 2 PUT 虽然返回 200，但 GeoServer 实际无法从该 URL 读取数据。

### 根因

`GeoServerProperties.java` 中 `minioUrl` 的默认值硬编码为 `http://minio:9000`：

```java
private String minioUrl = "http://minio:9000";
```

而应用已有的 `minio.endpoint` 配置已正确设置为 `http://localhost:9000`（在 `application.yml` 和 `application-local.yml` 中），但 `GeoServerProperties` 未从配置文件中读取该值。

### 修复目标

- 将 `geoserver.minio-url` 配置从硬编码改为可通过配置文件设定
- 默认值从 `http://minio:9000` 改为 `http://localhost:9000`（与 `minio.endpoint` 默认一致）
- 可选：允许通过环境变量 `GEOSERVER_MINIO_URL` 覆盖

## 影响范围

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `GeoServerProperties.java` | 修改 | `minioUrl` 默认值改为 `http://localhost:9000`；添加 `@Value` 绑定或 `application.yml` 配置项 |
| `application.yml` | 修改 | 在 `geoserver:` 下新增 `minio-url` 配置项 |
| `application-local.yml` | 修改 | 在 `geoserver:` 下新增 `minio-url` 配置项 |

## 风险评估

- 低风险：仅修改配置默认值，不影响业务逻辑
- 如果配置项未显式设置，使用新的合理默认值 `http://localhost:9000`
- 已在 `application-local.yml` 中显式设置时，覆盖默认值
