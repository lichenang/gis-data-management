# Tasks: fix-map-request-auth

## Task 1: 添加 getPublishedDatasets API 方法

**File**: `frontend/src/api/dataset.ts`

在文件末尾添加：
```typescript
export function getPublishedDatasets() {
  return get<{ code: number; data: Dataset[] }>('/datasets/published')
}
```

## Task 2: 修复 LayerPanel.vue 调用

**File**: `frontend/src/views/map/LayerPanel.vue`

1. 移除裸 axios 导入:
   - 删除: `import axios from 'axios'`

2. 更新 API 导入:
   - 添加: `getPublishedDatasets` 从 `@/api/dataset`

3. 修改 fetchPublishedLayers 函数:
   - 原: `const response = await axios.get('/api/v1/datasets/published')`
   - 改: `const response = await getPublishedDatasets()`
   - 原: `layers.value = response.data.data || []`
   - 改: `layers.value = response.data || []`

## Verification

1. 登录系统
2. 访问 `/map` 页面
3. 验证左侧图层列表显示已发布数据集
4. 验证网络请求携带 JWT Token
