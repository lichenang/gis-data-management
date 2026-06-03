# Design: add-dataset-publish-action

## Technical Design

### Backend

#### API Endpoints

```
PUT /api/v1/datasets/{id}/publish
- 将数据集 status 更新为 "published"
- 返回更新后的数据集

PUT /api/v1/datasets/{id}/unpublish
- 将数据集 status 更新为 "draft"
- 返回更新后的数据集
```

#### Implementation

在 `DatasetController.java` 中添加两个接口，调用 `DatasetService` 的方法更新 status 字段。

### Frontend

#### UI Changes

在 `datasets/index.vue` 的操作列中：
- 已发布状态：显示绿色"已发布"标签 + "取消发布"按钮
- 草稿状态：显示灰色"草稿"标签 + "发布"按钮

#### Button Logic

```vue
<el-button 
  :type="row.status === 'published' ? 'warning' : 'success'"
  link
  @click="handlePublish(row)">
  {{ row.status === 'published' ? '取消发布' : '发布' }}
</el-button>
```

#### API Calls

```typescript
async function publishDataset(id: number) {
  await axios.put(`/api/v1/datasets/${id}/publish`)
}

async function unpublishDataset(id: number) {
  await axios.put(`/api/v1/datasets/${id}/unpublish`)
}
```
