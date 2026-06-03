# 实现任务：修复影像发布功能

## 任务列表

- [x] ### 1. 添加 publishImage 和 unpublishImage API 函数

**文件**: `frontend/src/api/image.ts`

**修改内容**:

在文件末尾（`getImageWmsUrl` 函数之后）添加：

```typescript
export function publishImage(id: number) {
  return post<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}

export function unpublishImage(id: number) {
  return del<{ code: number; data: ImageDataset }>(`/images/${id}/publish`)
}
```

**导入说明**: 需要确保 `post` 和 `del` 函数已在文件顶部导入。

- [x] ### 2. 添加发布按钮

**文件**: `frontend/src/views/images/index.vue`

**修改内容**:

2.1 在 import 语句中添加 `publishImage` 和 `unpublishImage`：
```typescript
import { getImages, uploadImage, publishImage, unpublishImage, type ImageDataset } from '@/api/image'
```

2.2 在操作列添加按钮（参考 datasets/index.vue:93-97）：
```vue
<el-button
  :type="row.status === 'published' ? 'warning' : 'success'"
  link
  @click="handlePublish(row)"
>
  {{ row.status === 'published' ? '取消发布' : '发布' }}
</el-button>
```

2.3 添加事件处理函数（参考 datasets/index.vue:400-410）：
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
  } catch (error: any) {
    console.error('Publish failed:', error)
    ElMessage.error(error.message || '操作失败')
  }
}
```

- [x] ### 3. 验证修改

1. 检查 `publishImage` 和 `unpublishImage` 是否正确导入
2. 检查按钮样式是否与 datasets/index.vue 一致
3. 检查错误处理是否完善
