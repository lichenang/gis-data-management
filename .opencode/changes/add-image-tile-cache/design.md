# 设计方案：影像切片预缓存功能

## 技术方案

### 1. 后端 - GeoServerCacheService

新增 `getSeedStatus()` 方法查询 GWC 种子任务状态：

```java
public Map<String, Object> getSeedStatus(String workspace, String layerName) {
    String layerId = workspace + ":" + layerName;
    String url = "/rest/gwc/layers/" + layerId + "/seeds.json";

    try {
        String response = client.get(url, String.class);
        // 解析 JSON 获取状态和进度
        // 返回: {status, tilesTotal, tilesCached, progress}
    } catch (Exception e) {
        log.warn("Failed to get seed status for layer: {}", layerId, e);
        return null;
    }
}
```

### 2. 后端 - TileSeedService

修改 `triggerSeed()` 使用真实状态轮询替代 `simulateTilingProgress()`：

```
┌─────────────────────────────────────────────────────────────────┐
│                 新的 TileSeedService 流程                        │
└─────────────────────────────────────────────────────────────────┘

  triggerSeed(datasetId, minZoom, maxZoom)
         │
         ▼
  1. 设置 tileStatus="processing", cacheSeedStatus="seeding"
  2. 调用 cacheService.seedLayer() 触发 GWC 种子任务
  3. 启动定时轮询 (每 5 秒):
         │
         ├──► cacheService.getSeedStatus()
         │
         ├──► 更新 tileProgress, cacheSeedStatus
         │
         └──► 如果完成: tileStatus="completed", cacheSeedStatus="seeded"
              如果失败: tileStatus="failed", cacheSeedStatus="idle"
```

### 3. 前端 - image.ts

新增 API 函数：

```typescript
export function retileImage(id: number) {
  return post<{ code: number; data: any }>(`/images/${id}/retile`)
}
```

### 4. 前端 - images/index.vue

新增切片状态列（参考 spec 设计）：

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
    <el-tag v-else-if="row.tileStatus === 'failed'" type="danger">失败</el-tag>
    <el-tag v-else type="info">待处理</el-tag>
  </template>
</el-table-column>
```

新增"重新切片"按钮（当 tileStatus === 'completed' 时显示）。
