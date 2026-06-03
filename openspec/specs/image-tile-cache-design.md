# image-tile-cache-design - 影像切片预缓存方案

## 概述

当前 GeoServer 已集成 GeoWebCache，影像发布后 `wmsUrl` 和 `wmtsUrl` 已写入数据库。TileSeedService 会在发布时触发切片任务，但存在以下问题：

1. 切片进度是**模拟值**，未真正查询 GeoWebCache 状态
2. 前端缺少切片状态展示
3. 缺少手动重新切片功能

## 现状分析

```
┌─────────────────────────────────────────────────────────────────┐
│                      当前架构                                    │
└─────────────────────────────────────────────────────────────────┘

  ImageServiceImpl.publishImageDataset()
         │
         ▼
  TileSeedService.triggerSeed()  ← @Async 异步执行
         │
         ├──► GeoServerCacheService.seedLayer()
         │         │
         │         ▼
         │    POST /rest/gwc/layers/{layer}/seed  ← 触发 GWC 种子任务
         │
         └──► simulateTilingProgress()  ← ⚠️ 假进度，不是真实状态
                   │
                   ▼
              更新数据库 tileProgress (10%, 30%, 50%...)

  问题: 无法知道 GWC 实际切片进度
```

## 改进方案

### 1. 后端改进

#### 1.1 实现真实的 GeoWebCache 状态轮询

GeoWebCache 提供 REST API 查询种子任务状态：
```
GET /rest/gwc/layers/{workspace}:{layer}/seeds/{taskId}.json
```

返回格式：
```json
{
  "long": {
    "taskId": 1234,
    "status": "RUNNING",
    "tilesTotal": 1000000,
    "tilesCached": 450000,
    "progress": 45.0
  }
}
```

**实现方案**: 修改 `TileSeedService`，在触发种子任务后定时轮询状态并更新数据库。

#### 1.2 状态字段更新

Dataset 实体已有字段：
- `tileStatus`: pending/processing/completed/failed
- `tileProgress`: 0-100
- `cacheSeedStatus`: idle/seeding/seeded
- `tileJobId`: GeoWebCache 任务 ID

#### 1.3 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/images/{id}/tiling-status` | GET | 查询切片状态 |
| `/api/v1/images/{id}/retile` | POST | 手动重新切片 |
| `/api/v1/images/{id}/publish` | POST | 发布并触发切片（已存在） |

### 2. 前端改进

#### 2.1 影像管理页面

在 `images/index.vue` 表格中新增"切片状态"列：

```vue
<el-table-column label="切片状态" width="140">
  <template #default="{ row }">
    <el-tag v-if="row.tileStatus === 'completed'" type="success">
      已完成 ({{ row.tileProgress }}%)
    </el-tag>
    <el-progress
      v-else-if="row.tileStatus === 'processing'"
      :percentage="row.tileProgress"
      :stroke-width="10"
    />
    <el-tag v-else-if="row.tileStatus === 'failed'" type="danger">
      失败
    </el-tag>
    <el-tag v-else type="info">待处理</el-tag>
  </template>
</el-table-column>
```

#### 2.2 重新切片按钮

在切片状态列添加"重新切片"按钮：
```vue
<el-button
  v-if="row.tileStatus === 'completed'"
  type="warning"
  link
  size="small"
  @click="handleRetile(row)"
>
  重新切片
</el-button>
```

#### 2.3 事件处理

```typescript
const handleRetile = async (row: ImageDataset) => {
  try {
    await ElMessageBox.confirm('确定要重新生成切片吗？', '提示', {
      type: 'warning'
    })
    await retileImage(row.id!)
    ElMessage.success('重新切片任务已提交')
    fetchImages()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}
```

## 详细设计

### 后端 - TileSeedService 改造

```
┌─────────────────────────────────────────────────────────────────┐
│                 TileSeedService 流程                            │
└─────────────────────────────────────────────────────────────────┘

  triggerSeed(datasetId, minZoom, maxZoom)
         │
         ▼
  1. 设置 tileStatus="processing", cacheSeedStatus="seeding"
  2. 调用 cacheService.seedLayer() 触发 GWC 种子任务
  3. 获得 taskId
  4. 启动定时轮询 (每 5 秒):
         │
         ├──► GET /rest/gwc/layers/{layer}/seeds/{taskId}.json
         │
         ├──► 更新 tileProgress, cacheSeedStatus
         │
         └──► 如果完成: tileStatus="completed", cacheSeedStatus="seeded"
              如果失败: tileStatus="failed", cacheSeedStatus="idle"
```

### GeoWebCache 状态 API

```
GET /rest/gwc/layers/{workspace}:raster_{id}/seeds.json

响应:
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

### API 响应格式

**GET /api/v1/images/{id}/tiling-status**:
```json
{
  "code": 200,
  "data": {
    "status": "processing",
    "progress": 45,
    "cache_seed_status": "seeding",
    "tile_job_id": "uuid-xxx",
    "wms_url": "http://geoserver/wms",
    "wmts_url": "http://geoserver/wmts"
  }
}
```

## 数据库字段

Dataset 表已有字段（无需修改）：

| 字段 | 类型 | 说明 |
|------|------|------|
| tile_status | varchar | 状态: pending/processing/completed/failed |
| tile_progress | int | 进度 0-100 |
| cache_seed_status | varchar | GWC 状态: idle/seeding/seeded |
| tile_job_id | varchar | GWC 任务 ID |

## 实施步骤

### Phase 1: 后端状态轮询
1. 修改 `GeoServerCacheService` 添加 `getSeedStatus()` 方法
2. 修改 `TileSeedService` 实现真实状态轮询
3. 测试发布流程

### Phase 2: 前端展示
1. 在 `images/index.vue` 添加切片状态列
2. 添加"重新切片"按钮
3. 添加 API 函数 `retileImage()`

### Phase 3: 完善功能
1. 添加切片状态轮询（前端定时刷新）
2. 优化错误处理
3. 添加重试机制
