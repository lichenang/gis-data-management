# restructure-dataset-menu

## Why

当前左侧菜单中"数据集管理"是单一菜单项，同时包含矢量和影像两种类型。用户需要先进入页面再通过筛选器区分，操作不够直观。将数据集管理拆分为二级菜单可以提供更清晰导航。

## What Changes

将 `home/index.vue` 中的"数据集管理"菜单项改为展开式子菜单：
- 一级菜单：数据集管理（展开式，包含子菜单）
- 子菜单1：矢量数据集 → /datasets/vector
- 子菜单2：影像数据集 → /datasets/raster

保留 /datasets 路由并重定向到 /datasets/vector 实现向后兼容。

## Capabilities

### New Capabilities
- 清晰的矢量和影像数据集入口分离
- 通过 URL 直接访问特定类型数据集列表

### Changed Capabilities
- /datasets 路由现重定向到 /datasets/vector
- /datasets/vector 和 /datasets/raster 使用类型参数过滤数据集

## Impact

- 修改文件：
  - `frontend/src/views/home/index.vue` - 菜单结构调整
  - `frontend/src/router/index.ts` - 新增路由和重定向
  - `frontend/src/views/datasets/index.vue` - 支持类型参数过滤

## Non-goals

- 不修改数据集的增删改查功能
- 不修改影像管理页面功能（/images）
- 不添加新的图标组件
