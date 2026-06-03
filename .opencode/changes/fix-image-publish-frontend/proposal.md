# fix-image-publish-frontend

## 问题描述

影像发布功能前端部分缺失，导致用户无法通过 UI 发布影像数据集到 GeoServer。

## 根本原因

根据 `openspec/specs/debug-publish-flow.md` 的诊断报告：

1. **前端 API 缺失**: `frontend/src/api/image.ts` 缺少 `publishImage()` 和 `unpublishImage()` 函数
2. **前端页面缺失**: `frontend/src/views/images/index.vue` 缺少发布/取消发布按钮

后端 `ImageController` 已有完整的 `POST /images/{id}/publish` 和 `DELETE /images/{id}/publish` 接口，只是前端没有调用。

## 影响

- 用户无法通过影像管理页面上传后直接发布
- 必须通过其他途径（如 API 工具）发布影像
- 地图查看页面无法加载已发布的影像

## 预期结果

1. `image.ts` 包含 `publishImage()` 和 `unpublishImage()` API 函数
2. `images/index.vue` 表格操作列有"发布"/"取消发布"按钮
3. 点击按钮后状态正确更新，显示成功/失败消息
