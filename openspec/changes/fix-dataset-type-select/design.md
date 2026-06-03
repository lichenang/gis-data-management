## Context

当前 datasets/index.vue 页面用于管理所有数据集（矢量+影像），新建时显示类型选择。但实际使用中该页面主要用于矢量数据管理，影像有独立的 images 页面。

## Goals / Non-Goals

**Goals:**
- 隐藏新建对话框中的类型选择
- 默认 type='vector'

**Non-goals:**
- 不修改编辑功能（编辑时可修改类型）
- 不修改影像页面

## Implementation

在新建数据集的 el-form-item 中添加 `:hidden="!isEdit"` 或直接移除类型选择。默认 form.type = 'vector'。

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 需要创建矢量以外的数据 | 未来可能有需求 | 编辑时可修改类型 |
