# Tasks: fix-dataset-publish-auth

## Task 1: 添加 API 方法

**File**: `frontend/src/api/dataset.ts`

在文件末尾添加：
```typescript
export function publishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/publish`)
}

export function unpublishDataset(id: number) {
  return put<{ code: number; data: Dataset }>(`/datasets/${id}/unpublish`)
}
```

## Task 2: 修复组件调用

**File**: `frontend/src/views/datasets/index.vue`

1. 移除裸 axios 导入:
   - 删除: `import axios from 'axios'`

2. 更新 API 导入，添加新方法:
   - 原: `import { ..., type Dataset } from '@/api/dataset'`
   - 改: `import { ..., publishDataset, unpublishDataset, type Dataset } from '@/api/dataset'`

3. 更新 handlePublish 函数使用 API 方法:
   - 原: `await axios.put(\`/api/v1/datasets/${row.id}/publish\`)`
   - 改: `await publishDataset(row.id!)`
   - 原: `await axios.put(\`/api/v1/datasets/${row.id}/unpublish\`)`
   - 改: `await unpublishDataset(row.id!)`

## Verification

1. 启动后端服务
2. 启动前端服务
3. 登录系统
4. 进入数据集管理页面
5. 点击任意数据集的"发布"按钮
6. 验证请求成功，状态变为"已发布"
7. 点击"取消发布"按钮
8. 验证请求成功，状态变为"草稿"
