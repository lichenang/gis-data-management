# 实现任务：根据类型调用正确的发布API

## 任务列表

- [x] ### 1. 添加 image API 导入

**文件**: `frontend/src/views/datasets/index.vue`
**位置**: 第 258 行

**修改内容**:

在导入语句中添加 `publishImage` 和 `unpublishImage`:
```typescript
import { publishImage, unpublishImage } from '@/api/image'
```

- [x] ### 2. 修改 handlePublish 函数

**文件**: `frontend/src/views/datasets/index.vue`
**位置**: 第 398-414 行

**修改内容**:

将条件判断添加到发布/取消发布逻辑中:
```typescript
const isRaster = row.type === 'raster'
if (isPublished) {
  isRaster ? await unpublishImage(row.id!) : await unpublishDataset(row.id!)
  ElMessage.success('取消发布成功')
} else {
  isRaster ? await publishImage(row.id!) : await publishDataset(row.id!)
  ElMessage.success('发布成功')
}
```

- [x] ### 3. 验证修改

1. 检查 `publishImage` 和 `unpublishImage` 是否正确导入
2. 检查条件判断逻辑是否正确
3. 确保 raster 类型调用 images API，vector 类型调用 datasets API
