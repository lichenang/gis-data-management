## Why

数据集管理页面（datasets/index.vue）目前混合显示矢量数据和影像数据。但该页面主要用于矢量数据管理，新建时应默认 type='vector' 且隐藏类型选择，避免用户误选。

## What Changes

1. 修改 datasets/index.vue 新建数据集对话框，隐藏类型选择下拉框
2. 默认设置 type='vector'

## Capabilities

### New Capabilities
（无）

### Modified Capabilities
（无）

## Impact

- `frontend/src/views/datasets/index.vue` — 隐藏类型选择，默认 vector

## Non-goals

- 不修改已有数据的类型
- 不修改影像管理页面
