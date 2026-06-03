# Spec: 影像数据集发布与切片方案

## 1. 现状与问题

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        当前数据流程                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MinIO                   数据库                              GeoServer  │
│  ┌─────────┐            ┌───────────┐                       ┌────────┐ │
│  │ GeoTIFF │  upload    │  Dataset  │     publish           │  (未   │ │
│  │  .tif   │ ─────────► │ (status)  │ ──────────────────►   │  集成) │ │
│  └─────────┘            └───────────┘                       └────────┘ │
│                               │                                        │
│                               ▼                                        │
│                        ┌───────────────┐                               │
│                        │ raster_meta   │                               │
│                        │ (元数据)      │                               │
│                        └───────────────┘                               │
│                                                                          │
│  问题：                                                                  │
│  ❌ 发布仅为 status 字段变更，无实际 GeoServer 集成                      │
│  ❌ 影像数据无法通过 WMS/WMTS 访问                                       │
│  ❌ 无切片缓存机制                                                        │
│  ❌ 前端发布按钮无类型区分                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

## 2. 目标架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        目标数据流程                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MinIO                   后端服务                           GeoServer   │
│  ┌─────────┐            ┌───────────────┐                   ┌────────┐ │
│  │ GeoTIFF │  upload    │  ImageService │  publish          │Image   │ │
│  │  .tif   │ ─────────► │   .publish()  │ ────────────────► │Mosaic  │ │
│  └─────────┘            └───────┬───────┘                   └────────┘ │
│                                 │                               │       │
│                                 ▼                               │       │
│                        ┌───────────────┐                       │       │
│                        │ AsyncTask     │                       │       │
│                        │ (切片任务)    │ ◄─────────────────────┘       │
│                        └───────┬───────┘                               │
│                                │                                        │
│                ┌───────────────┼───────────────┐                       │
│                ▼               ▼               ▼                       │
│         ┌──────────┐   ┌──────────┐   ┌──────────┐                    │
│         │  WMS     │   │  WMTS    │   │ GeoWeb   │                    │
│         │  URL     │   │  URL     │   │Cache     │                    │
│         └──────────┘   └──────────┘   └──────────┘                    │
│                                                                          │
│  前端                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐      │
│  │  类型=vector → 调用 /api/v1/datasets/{id}/publish           │      │
│  │  类型=raster → 调用 /api/v1/images/{id}/publish + 进度显示  │      │
│  └──────────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────────┘
```

## 3. 技术方案

### 3.1 数据库变更

**dataset 表新增字段：**

```sql
-- 切片状态
ALTER TABLE dataset ADD COLUMN tile_status VARCHAR(20) DEFAULT 'pending';
-- pending: 待切片, processing: 切片中, completed: 切片完成, failed: 切片失败

-- 切片进度 (0-100)
ALTER TABLE dataset ADD COLUMN tile_progress INTEGER DEFAULT 0;

-- 切片任务ID (用于追踪)
ALTER TABLE dataset ADD COLUMN tile_job_id VARCHAR(64);

-- WMS 服务地址
ALTER TABLE dataset ADD COLUMN wms_url VARCHAR(500);

-- WMTS 服务地址
ALTER TABLE dataset ADD COLUMN wmts_url VARCHAR(500);

-- GeoWebCache 切片种子任务状态
ALTER TABLE dataset ADD COLUMN cache_seed_status VARCHAR(20) DEFAULT 'idle';
-- idle: 空闲, seeding: 正在生成, seeded: 完成
```

**可选：为 raster_metadata 增加切片信息关联：**

```sql
ALTER TABLE raster_metadata ADD COLUMN tile_levels VARCHAR(100);
-- 存储已生成的切片级别，如 "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18"
```

### 3.2 后端服务设计

```
backend/src/main/java/com/gisplatform/
├── config/
│   ├── GeoServerProperties.java          # GeoServer 配置属性
│   └── AsyncConfig.java                  # 异步任务线程池配置
│
├── service/
│   ├── ImageService.java                 # 扩展 publish() 方法
│   └── impl/ImageServiceImpl.java
│
├── service/geoserver/
│   ├── GeoServerClient.java              # GeoServer REST API 客户端
│   ├── GeoServerWorkspaceService.java    # 工作区管理
│   ├── GeoServerCoverageStoreService.java # 影像覆盖存储 (ImageMosaic)
│   ├── GeoServerLayerService.java        # 图层发布
│   └── GeoServerCacheService.java        # GeoWebCache 切片管理
│
├── service/tiling/
│   ├── TilingJobService.java             # 切片任务服务
│   ├── TilingJobExecutor.java            # 切片执行器
│   └── TilingStatusTracker.java          # 状态追踪
│
├── task/
│   ├── ImagePublishTask.java             # 发布异步任务
│   └── TileSeedTask.java                 # 切片种子任务
│
├── dto/
│   ├── ImagePublishRequest.java
│   ├── ImagePublishResponse.java
│   └── TilingStatus.java
│
└── controller/
    └── ImageController.java              # 扩展 /publish 和 /retile 端点
```

### 3.3 GeoServer REST API 集成

**创建 ImageMosaic 存储和工作区：**

```
POST /geoserver/rest/workspaces
POST /geoserver/rest/workspaces/{workspace}/coveragestores
POST /geoserver/rest/workspaces/{workspace}/coveragestores/{store}/external.imageMosaic
```

**发布图层：**

```
POST /geoserver/rest/workspaces/{workspace}/layers
```

**GeoWebCache 种子任务（自动切图）：**

```
POST /geoserver/rest/gwc/layers/{workspace}:{layer}
         - seed {bounds}, {zoomStart: 0}, {zoomEnd: 18}, {format: image/png}
```

### 3.4 异步切片流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      发布流程时序图                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Client         ImageService      GeoServerClient    Redis/DB          │
│    │                  │                    │                │          │
│    │ POST /publish   │                    │                │          │
│    │────────────────▶│                    │                │          │
│    │                 │ 1. 创建工作区       │                │          │
│    │                 │───────────────────▶│                │          │
│    │                 │◀───────────────────│                │          │
│    │                 │                    │                │          │
│    │                 │ 2. 创建Coverage   │                │          │
│    │                 │   Store (ImageMosaic)            │          │
│    │                 │───────────────────▶│                │          │
│    │                 │◀───────────────────│                │          │
│    │                 │                    │                │          │
│    │                 │ 3. 发布图层        │                │          │
│    │                 │───────────────────▶│                │          │
│    │                 │◀───────────────────│                │          │
│    │                 │                    │                │          │
│    │                 │ 4. 更新 DB (WMS/WMTS URL)          │          │
│    │                 │───────────────────────────────────▶│          │
│    │                 │                    │                │          │
│    │                 │ 5. 触发切片         │                │          │
│    │                 │───────────────────▶│                │          │
│    │                 │◀───────────────────│                │          │
│    │                 │                    │                │          │
│    │  返回: publish  │                    │                │          │
│    │    started      │                    │                │          │
│    │◀────────────────│                    │                │          │
│    │                 │                    │                │          │
│    │                 │     [异步] 切片执行中                │          │
│    │                 │───────────────────▶│                │          │
│    │                 │                    │                │          │
│    │                 │        ... 进度更新 ...             │          │
│    │                 │───────────────────────────────────▶│          │
│    │                 │                    │                │          │
│    │                 │                    │  切片完成       │          │
│    │                 │                    │◀───────────────│          │
│    │                 │                    │                │          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.5 前端变更

**数据集列表发布按钮改造：**

```vue
<!-- views/datasets/index.vue -->

<!-- 操作列 -->
<el-button
  :type="row.status === 'published' ? 'warning' : 'success'"
  :loading="publishingIds.includes(row.id)"
  link
  @click="handlePublish(row)"
>
  {{ getPublishButtonText(row) }}
</el-button>

<!-- 新增：切片进度显示 -->
<el-progress
  v-if="row.type === 'raster' && row.tile_status === 'processing'"
  :percentage="row.tile_progress"
  :stroke-width="4"
  style="margin-top: 4px"
/>
```

**JavaScript 逻辑：**

```ts
const getPublishButtonText = (row: Dataset) => {
  if (row.status === 'published') return '取消发布'

  if (row.type === 'raster') {
    switch (row.tile_status) {
      case 'processing': return '切片中...'
      case 'failed': return '重试切片'
      default: return '发布影像'
    }
  }
  return '发布'
}

const handlePublish = async (row: Dataset) => {
  if (row.type === 'raster') {
    await publishRaster(row.id)
  } else {
    await publishVector(row.id)
  }
}

const publishRaster = async (id: number) => {
  await publishImageDataset(id)
  // 轮询切片进度
  startTilingPolling(id)
}
```

**需要的 API 方法（dataset.ts 或 image.ts）：**

```ts
// image.ts
export function publishImageDataset(id: number) {
  return post(`/images/${id}/publish`)
}

export function unpublishImageDataset(id: number) {
  return put(`/images/${id}/unpublish`)
}

export function retileImageDataset(id: number) {
  return post(`/images/${id}/retile`)
}

export function getTilingStatus(id: number) {
  return get(`/images/${id}/tiling-status`)
}
```

## 4. 切片策略

### 4.1 级别与范围

| 级别 | 像素分辨率 | 典型应用 |
|------|-----------|----------|
| 0-5  | 全球/国家级 | 全景浏览 |
| 6-10 | 省级/市级 | 区域概览 |
| 11-14 | 区县级 | 城镇细节 |
| 15-18 | 街道级 | 精细查看 |

**默认配置：**

- 初始级别：0
- 最大级别：18 (可配置)
- 切片格式：PNG
- 缓存方式：GeoWebCache (内嵌于 GeoServer)

### 4.2 触发时机

| 场景 | 触发方式 |
|------|----------|
| 首次发布 | 自动触发，发布成功后异步执行 |
| 手动重新切片 | 调用 `/api/v1/images/{id}/retile` |
| 数据更新后 | 建议用户手动触发重新切片 |

### 4.3 进度追踪

- 存储到 dataset.tile_progress (0-100)
- 存储任务ID到 dataset.tile_job_id
- 前端轮询 `/tiling-status` 获取实时进度

## 5. 接口设计

### 5.1 发布影像数据集

```
POST /api/v1/images/{id}/publish

Response 202 Accepted:
{
  "code": 202,
  "data": {
    "status": "publishing",
    "tile_status": "processing",
    "tile_job_id": "img-123-abc",
    "message": "发布已开始，切片任务已提交"
  }
}

Response 400 Bad Request:
{
  "code": 400,
  "message": "数据集状态不允许发布"
}
```

### 5.2 取消发布

```
DELETE /api/v1/images/{id}/publish
# 或
PUT /api/v1/images/{id}/unpublish

Response:
{
  "code": 200,
  "data": {
    "status": "draft",
    "message": "已取消发布"
  }
}
```

### 5.3 手动重新切片

```
POST /api/v1/images/{id}/retile

Response 202 Accepted:
{
  "code": 202,
  "data": {
    "tile_job_id": "img-123-def",
    "message": "切片任务已重新提交"
  }
}
```

### 5.4 切片状态查询

```
GET /api/v1/images/{id}/tiling-status

Response:
{
  "code": 200,
  "data": {
    "status": "completed",  // pending/processing/completed/failed
    "progress": 100,        // 0-100
    "current_level": 18,
    "message": "切片完成，共生成 1234 个瓦片",
    "completed_at": "2026-05-28T10:30:00"
  }
}
```

## 6. 错误处理

| 场景 | 处理策略 |
|------|----------|
| GeoServer 连接失败 | 返回 503，服务不可用 |
| ImageMosaic 创建失败 | 回滚已创建内容，返回具体错误 |
| 切片任务失败 | 更新 tile_status=failed，记录错误信息，前端显示重试按钮 |
| MinIO 文件不存在 | 返回 404，提示数据已丢失 |

## 7. 实施计划

### Phase 1: 基础设施

1. 数据库迁移 (新增字段)
2. GeoServer 配置类
3. 异步任务线程池配置
4. GeoServer REST Client 基础

### Phase 2: 发布核心

1. ImageService.publish() 实现
2. GeoServer 工作区/存储/图层管理
3. 发布接口 POST /images/{id}/publish

### Phase 3: 切片

1. GeoWebCache 种子任务触发
2. 切片状态追踪与更新
3. 轮询接口 /tiling-status
4. 手动重切片 /retile

### Phase 4: 前端

1. 区分类型调用不同 API
2. 切片进度显示
3. 错误状态与重试

## 8. 不涉及范围

- Shapefile/ZIP 等多文件打包上传的 ImageMosaic（单 GeoTIFF 简化方案）
- PostGIS Raster 替代 ImageMosaic
- 动态 ImageMosaic 目录监控（ImageMosaic 已配置自动扫描）
- 矢量数据自动切片（现有方案不支持）
- 专题地图样式配置（YSLD/SLD）
