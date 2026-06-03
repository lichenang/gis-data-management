# 设计方案：根据类型调用正确的发布API

## 修改位置

**文件**: `frontend/src/views/datasets/index.vue`

## 修改内容

### 1. 添加 image API 导入

当前导入（第 258 行）:
```typescript
import { getDatasets, createDataset, updateDataset, deleteDataset, parseDatasetFile, importDataset, publishDataset, unpublishDataset, type Dataset, type GisDataParseResult } from '@/api/dataset'
```

添加 `publishImage` 和 `unpublishImage`:
```typescript
import { getDatasets, createDataset, updateDataset, deleteDataset, parseDatasetFile, importDataset, publishDataset, unpublishDataset, type Dataset, type GisDataParseResult } from '@/api/dataset'
import { publishImage, unpublishImage } from '@/api/image'
```

### 2. 修改 handlePublish 函数

当前实现（第 398-414 行）:
```typescript
const handlePublish = async (row: Dataset) => {
  try {
    const isPublished = row.status === 'published'
    const action = isPublished ? '取消发布' : '发布'
    await ElMessageBox.confirm(`确定要${action}该数据集吗？`, '提示', {
      type: 'warning'
    })
    if (isPublished) {
      await unpublishDataset(row.id!)
      ElMessage.success('取消发布成功')
    } else {
      await publishDataset(row.id!)
      ElMessage.success('发布成功')
    }
    fetchDatasets()
  } catch (error) {
    // ...
  }
}
```

修改后:
```typescript
const handlePublish = async (row: Dataset) => {
  try {
    const isPublished = row.status === 'published'
    const action = isPublished ? '取消发布' : '发布'
    await ElMessageBox.confirm(`确定要${action}该数据集吗？`, '提示', {
      type: 'warning'
    })
    const isRaster = row.type === 'raster'
    if (isPublished) {
      isRaster ? await unpublishImage(row.id!) : await unpublishDataset(row.id!)
      ElMessage.success('取消发布成功')
    } else {
      isRaster ? await publishImage(row.id!) : await publishDataset(row.id!)
      ElMessage.success('发布成功')
    }
    fetchDatasets()
  } catch (error) {
    // ...
  }
}
```

## 数据流

```
用户点击"发布"按钮
       │
       ▼
handlePublish(row)
       │
       ▼
┌─────────────────────────────┐
│ row.type === 'raster' ?     │
├─────────────────────────────┤
│   YES                       │   NO
│   ▼                         │   ▼
│ publishImage(row.id)        │ publishDataset(row.id)
│ unpublishImage(row.id)      │ unpublishDataset(row.id)
└─────────────────────────────┘
```
