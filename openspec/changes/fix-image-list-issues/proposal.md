# fix-image-list-issues

## Why

影像管理页面存在两个问题：

1. **切片状态不更新** - 用户点击"重新切片"后，进度一直显示 0% 或初始状态，需要手动刷新页面才能看到最新进度
2. **删除按钮无效** - 点击删除按钮后只显示成功提示，但实际未调用任何删除 API，数据未真正删除

## What Changes

### 1. 切片状态轮询更新

在 `images/index.vue` 中添加定时轮询机制：
- 组件挂载时启动轮询
- 每 5 秒检查一次正在切片的数据行
- 调用 `/api/v1/images/{id}/tiling-status` 获取最新状态并更新表格数据
- 当所有切片任务完成后停止轮询

### 2. 删除按钮功能修复

**后端** - 在 `ImageController.java` 添加删除接口：
- `DELETE /api/v1/images/{id}` - 删除影像数据集

**前端** - 修改 `image.ts` 和 `images/index.vue`：
- 添加 `deleteImage(id)` API 函数
- 修复 `handleDelete` 逻辑，调用实际删除 API

## Capabilities

### Fixed Capabilities
- 切片进度实时展示
- 影像数据正确删除

## Impact

- 修改文件：
  - `backend/src/main/java/com/gisplatform/controller/ImageController.java`
  - `frontend/src/api/image.ts`
  - `frontend/src/views/images/index.vue`

## Non-goals

- 不修改切片触发的其他逻辑
- 不修改其他列表页的删除功能
