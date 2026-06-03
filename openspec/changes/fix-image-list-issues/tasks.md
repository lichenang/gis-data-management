## 1. 后端 - 添加删除接口

- [x] 1.1 在 `ImageController.java` 添加 `@DeleteMapping("/{id}")` 端点
  - 调用 `imageService.deleteImage(id)`
  - 返回 `R.ok()` 或 `R.fail(message)`

## 2. 前端 - 添加 API 函数

- [x] 2.1 在 `image.ts` 添加 `deleteImage(id)` 函数
  - 使用 `del()` 方法
  - 路径: `/images/${id}`

- [x] 2.2 在 `image.ts` 添加 `getTilingStatus(id)` 函数
  - 使用 `get()` 方法
  - 路径: `/images/${id}/tiling-status`

## 3. 前端 - 修复删除功能

- [x] 3.1 在 `images/index.vue` 的 `handleDelete` 函数中添加 `deleteImage(row.id!)` 调用

- [x] 3.2 在 `images/index.vue` 中导入 `deleteImage` 和 `getTilingStatus`

## 4. 前端 - 添加切片状态轮询

- [x] 4.1 添加 `tilingStatusTimer` 状态变量

- [x] 4.2 添加 `startTilingStatusPolling()` 函数
  - 每 5 秒轮询一次
  - 只处理 `tileStatus === 'processing'` 的行
  - 调用 `getTilingStatus` 更新状态

- [x] 4.3 添加 `stopTilingStatusPolling()` 函数

- [x] 4.4 在 `onMounted` 中调用 `startTilingStatusPolling()`

- [x] 4.5 添加 `onUnmounted` 生命周期钩子调用 `stopTilingStatusPolling()`

## 5. 验证

- [ ] 5.1 访问影像管理页面，确认切片状态列正常显示
- [ ] 5.2 点击"重新切片"，确认切片进度实时更新
- [ ] 5.3 点击删除按钮，确认数据被正确删除
