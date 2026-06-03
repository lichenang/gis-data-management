## Why

当前 datasets/index.vue（矢量管理）和 images/index.vue（影像管理）页面在新建/编辑时都显示"类型"下拉框，允许用户切换类型。这是职责不清的体现：

1. datasets 页面用于矢量数据管理，新建时不应选择影像
2. images 页面用于影像数据管理，新建时不应选择矢量
3. 编辑已存在的数据集时，类型字段不应允许修改

## What Changes

1. **datasets/index.vue**：
   - 新建表单初始化时固定 `type = 'vector'`，隐藏"类型"下拉框
   - 编辑模式下类型字段禁用（`:disabled="true"`）
2. **images/index.vue**：
   - 新建表单初始化时固定 `type = 'raster'`，隐藏"类型"下拉框
   - 编辑模式下类型字段禁用

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `frontend/src/views/datasets/index.vue` — 隐藏类型选择，默认 vector
- `frontend/src/views/images/index.vue` — 隐藏类型选择，默认 raster

## Non-goals

- 不修改后端接口逻辑
- 不修改数据库表结构
- 不修改现有数据的类型字段
