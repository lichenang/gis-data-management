# Tasks: add-image-publish

## Task 1: 数据库迁移

**文件**: 数据库迁移脚本

创建 Flyway 迁移文件：

```
backend/src/main/resources/db/migration/V4__add_image_publish_fields.sql
```

```sql
ALTER TABLE dataset ADD COLUMN tile_status VARCHAR(20) DEFAULT 'pending';
ALTER TABLE dataset ADD COLUMN tile_progress INTEGER DEFAULT 0;
ALTER TABLE dataset ADD COLUMN tile_job_id VARCHAR(64);
ALTER TABLE dataset ADD COLUMN wms_url VARCHAR(500);
ALTER TABLE dataset ADD COLUMN wmts_url VARCHAR(500);
ALTER TABLE dataset ADD COLUMN cache_seed_status VARCHAR(20) DEFAULT 'idle';
```

## Task 2: 扩展 Dataset 实体

**文件**: `backend/src/main/java/com/gisplatform/entity/Dataset.java`

添加字段和 getter/setter：
```java
private String tileStatus;
private Integer tileProgress;
private String tileJobId;
private String wmsUrl;
private String wmtsUrl;
private String cacheSeedStatus;
```

## Task 3: 配置类 - AsyncConfig

**文件**: `backend/src/main/java/com/gisplatform/config/AsyncConfig.java`

创建线程池配置类：
- @Configuration + @EnableAsync
- TaskExecutor bean 命名为 "tilingExecutor"
- 可配置 corePoolSize, maxPoolSize, queueCapacity

## Task 4: 配置类 - GeoServerProperties

**文件**: `backend/src/main/java/com/gisplatform/config/GeoServerProperties.java`

- @ConfigurationProperties(prefix = "geoserver")
- 字段: url, username, password, workspace, tilingMinZoom, tilingMaxZoom

application.yml 已存在 geoserver 配置，无需修改 yml。

## Task 5: GeoServerClient 基础类

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerClient.java`

创建基础 REST 客户端：
- 注入 GeoServerProperties
- RestTemplate (或 WebClient) 处理 HTTP 请求
- Basic Auth 认证头
- 通用方法: exchange(path, method, body, responseType)

## Task 6: GeoServerWorkspaceService

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerWorkspaceService.java`

方法：
- `createWorkspace(String name)` - POST /rest/workspaces
- `workspaceExists(String name)` - GET /rest/workspaces/{ws}

## Task 7: GeoServerCoverageStoreService

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCoverageStoreService.java`

方法：
- `createImageMosaicStore(String workspace, String storeName, String filePath, int minZoom, int maxZoom)` - POST /rest/workspaces/{ws}/coveragestores + external.imageMosaic
- `storeExists(String workspace, String storeName)` - GET /rest/workspaces/{ws}/coveragestores/{store}

## Task 8: GeoServerLayerService

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerLayerService.java`

方法：
- `publishLayer(String workspace, String storeName, String layerName, String title)` - POST /rest/workspaces/{ws}/layers
- `unpublishLayer(String workspace, String layerName)` - DELETE /rest/workspaces/{ws}/layers/{layer}

## Task 9: GeoServerCacheService

**文件**: `backend/src/main/java/com/gisplatform/service/geoserver/GeoServerCacheService.java`

方法：
- `seedLayer(String workspace, String layerName, int minZoom, int maxZoom)` - POST /rest/gwc/layers/{ws}:{layer}/seed
- `getSeedStatus(String workspace, String layerName)` - GET /rest/gwc/layers/{ws}:{layer}/seed/{taskId}

## Task 10: TileSeedService

**文件**: `backend/src/main/java/com/gisplatform/service/tiling/TileSeedService.java`

- @Async("tilingExecutor")
- `triggerSeed(Long datasetId, int minZoom, int maxZoom)` - 主入口
- `monitorSeedProgress(Long datasetId)` - 轮询 GeoWebCache 状态并更新数据库
- 异常处理：失败时更新 tile_status=failed, cache_seed_status=idle

## Task 11: ImageService 接口扩展

**文件**: `backend/src/main/java/com/gisplatform/service/ImageService.java`

添加方法签名：
```java
Dataset publishImageDataset(Long id);
Dataset unpublishImageDataset(Long id);
String triggerRetile(Long id);
Map<String, Object> getTilingStatus(Long id);
```

## Task 12: ImageServiceImpl 实现

**文件**: `backend/src/main/java/com/gisplatform/service/impl/ImageServiceImpl.java`

实现 Task 11 添加的所有方法，引入 GeoServer 服务和 TileSeedService。

publishImageDataset() 核心逻辑：
1. 校验数据集存在且状态为 draft
2. 创建/检查工作区
3. 创建 ImageMosaic 存储 (使用 MinIO 文件路径)
4. 发布图层
5. 计算并保存 WMS/WMTS URL
6. 更新 dataset 状态
7. 调用 TileSeedService.triggerSeed() 异步切片

## Task 13: ImageController 端点扩展

**文件**: `backend/src/main/java/com/gisplatform/controller/ImageController.java`

添加端点：
- `POST /{id}/publish` - 发布影像
- `DELETE /{id}/publish` 或 `PUT /{id}/unpublish` - 取消发布
- `POST /{id}/retile` - 手动重新切片
- `GET /{id}/tiling-status` - 查询切片状态

## Task 14: 验证编译

**命令**:
```bash
cd backend && mvn compile
```

确认无编译错误。

## Task 15: 数据库迁移验证

运行 Flyway 迁移，确认 V4 脚本执行成功，dataset 表新增字段存在。
