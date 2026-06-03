# 诊断报告: 影像管理页面两个问题

## 问题概述

1. **切片状态一直显示 0%，不自动更新** - 需要手动刷新页面才能看到最新进度
2. **删除按钮点击后无法删除数据** - 点击后只显示成功消息，但实际未调用删除 API

---

## 问题 1: 切片状态不自动更新

### 根因分析

**前端代码 (images/index.vue)**

```javascript
// Line 346-348 - onMounted 只调用一次 fetchImages
onMounted(() => {
  fetchImages()
})

// Line 200-216 - fetchImages 获取影像列表，但不包含实时切片状态
const fetchImages = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, pageSize: pagination.pageSize, name: searchForm.name || undefined }
    const res = await getImages(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('Failed to fetch images:', error)
  } finally {
    loading.value = false
  }
}
```

**问题**:
1. `fetchImages()` 只在组件挂载时调用一次，没有定时轮询机制
2. 切片状态 (`tileStatus`, `tileProgress`) 需要通过单独的状态查询接口 `/api/v1/images/{id}/tiling-status` 获取
3. 当用户触发切片后，前端立即调用 `fetchImages()` 但后端切片任务刚启动，状态仍为初始值

### 修复方案

添加切片状态轮询机制：

```javascript
// 1. 添加定时器状态
const tilingStatusTimer = ref<NodeJS.Timeout | null>(null)

// 2. 启动轮询（当检测到有 processing 状态的行时）
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
  }, 5000) // 每 5 秒轮询
}

const stopTilingStatusPolling = () => {
  if (tilingStatusTimer.value) {
    clearInterval(tilingStatusTimer.value)
    tilingStatusTimer.value = null
  }
}

// 3. onMounted 时启动轮询，onUnmounted 时停止
onMounted(() => {
  fetchImages()
  startTilingStatusPolling()
})

onUnmounted(() => {
  stopTilingStatusPolling()
})
```

---

## 问题 2: 删除按钮无法删除数据

### 根因分析

**前端代码 (images/index.vue:283-295)**

```javascript
const handleDelete = async (row: ImageDataset) => {
  try {
    await ElMessageBox.confirm('确定要删除该影像吗？', '提示', {
      type: 'warning'
    })
    ElMessage.success('删除成功')  // ← 直接显示成功，没有调用任何 API！
    fetchImages()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}
```

**问题**: `handleDelete` 缺少实际的删除 API 调用！

**API 文件 (image.ts)**

```typescript
// image.ts 中没有 deleteImage 函数！
export function getImages(params: { page?: number; pageSize?: number; name?: string }) { ... }
export function uploadImage(file: File, name?: string, description?: string) { ... }
export function publishImage(id: number) { ... }
export function unpublishImage(id: number) { ... }
export function retileImage(id: number) { ... }
// 缺少: export function deleteImage(id: number) { ... }
```

**后端 Controller (ImageController.java)**

```java
// ImageController.java 中没有 @DeleteMapping("/{id}") 端点！
@GetMapping              // 获取列表
@PostMapping("/upload")  // 上传
@GetMapping("/{id}")     // 获取详情
@PostMapping("/{id}/publish")    // 发布
@DeleteMapping("/{id}/publish")  // 取消发布
@PostMapping("/{id}/retile")     // 重新切片
@GetMapping("/{id}/tiling-status") // 获取切片状态
// 缺少: @DeleteMapping("/{id}")  ← 删除端点不存在
```

### 修复方案

**1. 添加后端删除接口**

在 `ImageController.java` 添加：

```java
@DeleteMapping("/{id}")
@Operation(summary = "删除影像数据集", description = "根据ID删除影像数据集")
public R<Void> delete(@Parameter(description = "数据集ID") @PathVariable Long id) {
    try {
        imageService.deleteImage(id);
        return R.ok();
    } catch (Exception e) {
        return R.fail(e.getMessage());
    }
}
```

**2. 添加前端 API 函数**

在 `image.ts` 添加：

```typescript
export function deleteImage(id: number) {
  return del<{ code: number }>(`/images/${id}`)
}
```

**3. 修复 handleDelete 函数**

```javascript
import { getImages, uploadImage, publishImage, unpublishImage, retileImage, deleteImage, type ImageDataset } from '@/api/image'

const handleDelete = async (row: ImageDataset) => {
  try {
    await ElMessageBox.confirm('确定要删除该影像吗？', '提示', {
      type: 'warning'
    })
    await deleteImage(row.id!)  // ← 添加实际的删除 API 调用
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

---

## 架构图示

```
┌─────────────────────────────────────────────────────────────────┐
│                    Issue 1: 切片状态不更新                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  当前流程:                                                       │
│  用户点击"重新切片"                                              │
│       │                                                         │
│       ▼                                                         │
│  handleRetile() ──► retileImage(id) ──► fetchImages()          │
│       │                                    │                    │
│       │                                    ▼                    │
│       │                            tableData 更新但             │
│       │                            tileStatus 还是初始值        │
│       │                                    │                    │
│       │                                    ▼                    │
│       │                            切片在后台进行中...           │
│       │                                    │                    │
│       │                                    ▼                    │
│       │                            需要手动刷新页面才能看到状态   │
│                                                                 │
│  期望流程:                                                       │
│  用户点击"重新切片"                                              │
│       │                                                         │
│       ▼                                                         │
│  handleRetile() ──► 启动定时轮询 ──► 定时调用 tiling-status API │
│                              │                    │             │
│                              ▼                    ▼             │
│                       定时更新 tableData    实时显示切片进度     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Issue 2: 删除按钮无效                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  handleDelete()                                                 │
│       │                                                         │
│       ├──► ElMessageBox.confirm()  ← 显示确认框                 │
│       │                                                         │
│       ├──► ElMessage.success('删除成功')  ← 直接显示成功！      │
│       │                                                         │
│       └──► fetchImages()  ← 刷新列表（但数据实际未删除）        │
│                                                                 │
│  ❌ 缺少: deleteImage(row.id!) API 调用                         │
│                                                                 │
│  后端 ImageController.java 没有 @DeleteMapping("/{id}")         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 修改文件清单

| 序号 | 文件 | 问题 | 修复方式 |
|------|------|------|----------|
| 1 | `ImageController.java` | 缺少删除端点 | 添加 `@DeleteMapping("/{id}")` |
| 2 | `image.ts` | 缺少 deleteImage 函数 | 添加 `export function deleteImage()` |
| 3 | `images/index.vue` | handleDelete 未调用删除 API | 添加 `await deleteImage(row.id!)` |
| 4 | `images/index.vue` | 缺少状态轮询 | 添加 `startTilingStatusPolling()` |
