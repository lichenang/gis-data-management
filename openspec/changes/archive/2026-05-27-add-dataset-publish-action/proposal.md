# Proposal: add-dataset-publish-action

## Summary

为数据集管理页面增加"发布"和"取消发布"功能，允许用户将数据集从"草稿"状态发布到"已发布"状态，或从"已发布"状态取消发布回"草稿"状态。已发布的数据集将在地图查看模块中显示。

## Problem Statement

当前数据集管理页面缺少发布功能，用户无法将创建的数据集发布到地图查看模块供其他用户浏览。

## Goals

1. 后端：新增发布和取消发布接口
2. 前端：在数据集列表操作列增加发布/取消发布按钮
3. 状态变更后自动刷新列表

## Success Criteria

- `PUT /api/v1/datasets/{id}/publish` 接口正常返回
- `PUT /api/v1/datasets/{id}/unpublish` 接口正常返回
- 前端操作列根据状态显示对应按钮
- 状态变更后列表自动刷新
