# 设计: fix-image-list-issues

## 问题 1: 切片状态不更新

### 当前问题

`images/index.vue` 只在 `onMounted` 时调用一次 `fetchImages()`，切片状态不会自动刷新。

### 修改方案

```javascript
// 1. 添加定时器状态
const tilingStatusTimer = ref<NodeJS.Timeout | null>(null)

// 2. 启动轮询函数
const startTilingStatusPolling = () => {
  stopTilingStatusPolling()
  tilingStatusTimer.value = setInterval(async () => {
    const processingRows = tableData.value.filter(row => row.tileStatus === 'processing')
    if (processingRows.length === 0) {
      stopTilingStatusPolling()
      return
    }
    for (const row of processingRows) {
      try {
        const res = await getTilingStatus(row.id)
        if (res.data) {
          row.tileStatus = res.data.status
          row.tileProgress = res.data.progress
        }
      } catch (error) {
        console.error('Failed to fetch tiling status:', error)
      }
    }
  }, 5000)
}

const stopTilingStatusPolling = () => {
  if (tilingStatusTimer.value) {
    clearInterval(tilingStatusTimer.value)
    tilingStatusTimer.value = null
  }
}

// 3. 在 onMounted 启动，onUnmounted 停止
onMounted(() => {
  fetchImages()
  startTilingStatusPolling()
})

onUnmounted(() => {
  stopTilingStatusPolling()
})
```

## 问题 2: 删除按钮无效

### 当前问题

`handleDelete` 函数缺少实际删除 API 调用：

```javascript
// 当前代码（错误）
const handleDelete = async (row) => {
  await ElMessageBox.confirm(...)
  ElMessage.success('删除成功')  // ← 直接显示成功，未调用删除 API
  fetchImages()
}
```

### 后端修改 - ImageController.java

添加删除接口：

```java
@DeleteMapping("/{id}")
@Operation(summary = "删除影像数据集", description = "根据ID删除影像数据集")
public R<Void> delete(@PathVariable Long id) {
    try {
        imageService.deleteImage(id);
        return R.ok();
    } catch (Exception e) {
        return R.fail(e.getMessage());
    }
}
```

### 前端修改 - image.ts

添加 `deleteImage` 函数：

```typescript
export function deleteImage(id: number) {
  return del<{ code: number }>(`/images/${id}`)
}
```

### 前端修改 - images/index.vue

```javascript
import { getImages, uploadImage, publishImage, unpublishImage, retileImage, deleteImage, type ImageDataset } from '@/api/image'

// 添加 tiling-status API
import { getTilingStatus } from '@/api/image'  // 需要添加

const handleDelete = async (row: ImageDataset) => {
  try {
    await ElMessageBox.confirm('确定要删除该影像吗？', '提示', {
      type: 'warning'
    })
    await deleteImage(row.id!)  // ← 添加实际删除调用
    ElMessage.success('删除成功')
    fetchImages()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}
```

## 验证步骤

1. 访问影像管理页面，确认切片状态列正常显示
2. 点击"重新切片"，确认切片进度实时更新
3. 点击删除按钮，确认数据被正确删除
