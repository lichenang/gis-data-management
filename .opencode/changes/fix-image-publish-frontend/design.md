# 设计方案：修复影像发布功能

## 修改范围

### 1. frontend/src/api/image.ts

添加两个 API 函数：

```typescript
export function publishImage(id: number) {
  return post<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}

export function unpublishImage(id: number) {
  return del<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}
```

参考 `dataset.ts` 中 `publishDataset()` 和 `unpublishDataset()` 的实现。

### 2. frontend/src/views/images/index.vue

在表格操作列添加发布/取消发布按钮：

```vue
<el-button
  :type="row.status === 'published' ? 'warning' : 'success'"
  link
  @click="handlePublish(row)"
>
  {{ row.status === 'published' ? '取消发布' : '发布' }}
</el-button>
```

添加对应的事件处理函数：

```typescript
const handlePublish = async (row: ImageDataset) => {
  try {
    if (row.status === 'published') {
      await unpublishImage(row.id!)
      ElMessage.success('取消发布成功')
    } else {
      await publishImage(row.id!)
      ElMessage.success('发布成功')
    }
    fetchImages()
  } catch (error) {
    console.error('Publish failed:', error)
    ElMessage.error(error.message || '操作失败')
  }
}
```

## 界面参考

参考 `frontend/src/views/datasets/index.vue` 第 93-97 行和 400-410 行的实现。
