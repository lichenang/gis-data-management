# Design: fix-map-request-auth

## Technical Design

### Problem

`LayerPanel.vue` 组件使用裸 axios 调用后端接口：

```typescript
// 错误代码 (当前)
import axios from 'axios'
const response = await axios.get('/api/v1/datasets/published')
// 请求不携带 JWT Token，导致 403
```

### Solution

#### 1. 添加 API 方法

在 `frontend/src/api/dataset.ts` 中添加：

```typescript
export function getPublishedDatasets() {
  return get<{ code: number; data: Dataset[] }>('/datasets/published')
}
```

#### 2. 修改组件调用

在 `frontend/src/views/map/LayerPanel.vue` 中：

```typescript
// 移除裸 axios
import axios from 'axios'  // 删除

// 添加 API 导入
import { getPublishedDatasets } from '@/api/dataset'

// 修改调用方式
const response = await getPublishedDatasets()
```

## Files to Modify

1. `frontend/src/api/dataset.ts` - 添加 `getPublishedDatasets()` 方法
2. `frontend/src/views/map/LayerPanel.vue` - 使用配置好的 API 方法
