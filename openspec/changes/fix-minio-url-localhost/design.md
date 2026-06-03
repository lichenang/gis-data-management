# Design: fix-minio-url-localhost

## 当前状态

### GeoServerProperties.java

```java
@ConfigurationProperties(prefix = "geoserver")
public class GeoServerProperties {
    private String minioUrl = "http://minio:9000";     // ← Docker 内部主机名，外部不可达
    private String minioBucket = "gis-raster";
}
```

### GeoServerCoverageStoreService.createImageMosaicStore()

```java
String fileUrl = props.getMinioUrl() + "/" + minioBucket + "/" + minioKey;
// 结果: http://minio:9000/gis-raster/images/uuid.tif  ← minio 不可解析
```

### application.yml (geoserver section)

当前 `geoserver:` 下没有 `minio-url` 配置项，`minioUrl` 字段通过 `@ConfigurationProperties` 绑定前缀 `geoserver`，会尝试从 `geoserver.minio-url` 读取。由于配置文件中不存在该键，使用硬编码默认值。

## 修复方案

### 方案选型

**方案 A**：仅修改默认值（最简单）

将 `GeoServerProperties.minioUrl` 的默认值从 `http://minio:9000` 改为 `http://localhost:9000`。

```java
private String minioUrl = "http://localhost:9000";
```

优点：一行改动，无需修改配置文件。
缺点：默认值固化在代码中，不灵活。

**方案 B**：添加配置项（推荐）

在三个文件中添加配置项，形成完整链路：

1. `application.yml` — 添加默认值，支持环境变量覆盖
2. `application-local.yml` — 添加本地显式值
3. `GeoServerProperties.java` — 默认值改为与 `application.yml` 一致（兜底）

推荐方案 B，因为：
- 与其他配置（如 `geoserver.url`）风格一致
- 支持通过环境变量 `GEOSERVER_MINIO_URL` 覆盖
- 本地开发和部署环境可使用不同值

### 具体变更

#### `application.yml` (geoserver section)

```yaml
geoserver:
  url: ${GEOSERVER_URL:http://localhost:8080/geoserver}
  minio-url: ${GEOSERVER_MINIO_URL:http://localhost:9000}    # ← 新增
```

#### `application-local.yml` (geoserver section)

```yaml
geoserver:
  url: http://localhost:8080/geoserver
  minio-url: http://localhost:9000    # ← 新增
```

#### `GeoServerProperties.java`

```java
// 无需修改代码绑定，@ConfigurationProperties(prefix = "geoserver") 会自动将
// geoserver.minio-url 映射到 minioUrl 字段（kebab-case → camelCase）
// 仅修改默认值与 application.yml 保持一致
private String minioUrl = "http://localhost:9000";
```

### 验证方法

发布一条影像数据，检查 GeoServer 日志：
- GeoServer 成功从 `http://localhost:9000/gis-raster/...` 读取 GeoTIFF 文件
- Coverage store 和图层正确创建
- 图层预览正常显示

## 前置条件

- MinIO 服务运行在本机 9000 端口（或通过 `localhost:9000` 可达）

## 后置条件

- 新发布的影像 URL 格式为 `http://localhost:9000/gis-raster/...`
- Docker 部署时可通过 `GEOSERVER_MINIO_URL` 环境变量覆盖
