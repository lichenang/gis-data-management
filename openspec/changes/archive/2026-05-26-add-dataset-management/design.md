# 设计: add-dataset-management

## 后端 API

### REST API

| 方法 | 路径 | 参数 |
|------|------|------|
| GET | /api/v1/datasets | page, pageSize, name?, type?, status? |
| GET | /api/v1/datasets/{id} | - |
| POST | /api/v1/datasets | name, description, type |
| PUT | /api/v1/datasets/{id} | name, description, type |
| DELETE | /api/v1/datasets/{id} | - |

### 数据结构

Dataset 实体字段：
- id, name, description, type (vector/raster), geometryType, srs
- storageType (postgis/minio), tableName, minioKey
- extent (JSONB), featureCount
- status (draft/published), version
- workspace, storeName, layerName
- tags (JSONB), createdBy
- tenantId, createTime, updateTime, deleted

## 前端

### 路由

```typescript
{
  path: '/datasets',
  name: 'Datasets',
  component: () => import('@/views/datasets/index.vue'),
  meta: { title: '数据集管理', requiresAuth: true }
}
```

### 页面结构

- 搜索栏：名称搜索 + 类型筛选 + 状态筛选
- 表格：名称、类型、几何、要素数、状态、创建时间
- 分页：el-pagination
- 操作：新建、编辑、删除

### API 封装

```typescript
// frontend/src/api/dataset.ts
export function getDatasets(params)
export function getDataset(id)
export function createDataset(data)
export function updateDataset(id, data)
export function deleteDataset(id)
```

## 文件清单

### 后端
- `backend/src/main/java/com/gisplatform/entity/Dataset.java`
- `backend/src/main/java/com/gisplatform/mapper/DatasetMapper.java`
- `backend/src/main/java/com/gisplatform/service/DatasetService.java`
- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`
- `backend/src/main/java/com/gisplatform/controller/DatasetController.java`

### 前端
- `frontend/src/router/index.ts` (添加 /datasets 路由)
- `frontend/src/api/dataset.ts` (API 封装)
- `frontend/src/views/datasets/index.vue` (列表页)
