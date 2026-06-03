# Design: fix-dataset-publish-auth

## Technical Design

### Problem Analysis

| 调用方式 | 是否携带 JWT | 结果 |
|---------|-------------|------|
| `axios.put('/api/v1/...')` | ❌ 否 | 403 Forbidden |
| `put('/datasets/...')` from `@/api/request` | ✅ 是 | 200 OK |

### Solution

#### 1. API 方法封装

在 `frontend/src/api/dataset.ts` 中添加：

```typescript
export function publishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/publish`)
}

export function unpublishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/unpublish`)
}
```

#### 2. 组件调用修改

在 `frontend/src/views/datasets/index.vue` 中：

```typescript
// 移除裸 axios
// import axios from 'axios'  ← 删除

// 添加 API 方法导入
import { getDatasets, createDataset, updateDataset, deleteDataset, parseDatasetFile, importDataset, publishDataset, unpublishDataset, type Dataset } from '@/api/dataset'

// 修改 handlePublish 函数
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
    if (error !== 'cancel') {
      console.error('Publish failed:', error)
    }
  }
}
```

### API Flow

```
User clicks "发布" button
        │
        ▼
handlePublish(row)
        │
        ▼
publishDataset(id)  ← Use configured API
        │
        ▼
axios.put('/datasets/{id}/publish')
        │
        ▼
Request Interceptor adds:
  Authorization: Bearer <jwt_token>
        │
        ▼
Backend returns 200 OK
```
