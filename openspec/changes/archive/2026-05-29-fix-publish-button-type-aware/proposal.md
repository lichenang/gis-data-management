# fix-publish-button-type-aware

## 问题描述

`datasets/index.vue` 的发布按钮对所有类型数据集都调用 `publishDataset()` 接口，但影像数据集 (type='raster') 应该调用 `publishImage()` 接口（路径为 `/api/v1/images/{id}/publish`）。

## 根本原因

当前 `handlePublish` 函数实现:
```typescript
const handlePublish = async (row: Dataset) => {
  if (isPublished) {
    await unpublishDataset(row.id!)  // 始终调用矢量接口
  } else {
    await publishDataset(row.id!)    // 始终调用矢量接口
  }
}
```

正确的实现应该是:
```typescript
const handlePublish = async (row: Dataset) => {
  const isRaster = row.type === 'raster'
  if (isPublished) {
    isRaster ? await unpublishImage(row.id!) : await unpublishDataset(row.id!)
  } else {
    isRaster ? await publishImage(row.id!) : await publishDataset(row.id!)
  }
}
```

## 影响

- 用户在数据集管理页面点击影像的"发布"按钮
- 调用了错误的后端接口 `/api/v1/datasets/{id}/publish` 而非 `/api/v1/images/{id}/publish`
- 发布操作失败或行为异常

## 预期结果

- `handlePublish` 根据 `row.type` 调用正确的 API
- type='vector' → 调用 `publishDataset/unpublishDataset`
- type='raster' → 调用 `publishImage/unpublishImage`
