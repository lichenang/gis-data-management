# fix-menu-title-and-raster-route

## Why

菜单重构（restructure-dataset-menu）后存在两个 UI 问题：

1. **页面标题未区分数据类型** - 矢量数据集和影像数据集页面内标题都显示"数据集管理"，与路由 meta.title 不一致
2. **影像切片功能缺失** - /datasets/raster 复用 datasets/index.vue，但该页面缺少切片管理功能（切片状态、重新切片按钮）

## What Changes

1. **修改 datasets/index.vue 页面标题**
   - 从硬编码"数据集管理"改为基于 `route.path` 动态显示
   - /datasets/vector → "矢量数据集"
   - /datasets/raster → "影像数据集"
   - 其他路径 → "数据集管理"

2. **修改 /datasets/raster 路由**
   - 从指向 `datasets/index.vue` 改为重定向到 `/images`
   - 复用 images/index.vue 的完整切片管理功能

## Capabilities

### Fixed Capabilities
- 页面标题与路由一致，用户明确知道自己所在位置
- 影像数据集页面具备完整的切片管理功能

## Impact

- 修改文件：
  - `frontend/src/views/datasets/index.vue` - 标题动态化
  - `frontend/src/router/index.ts` - /datasets/raster 重定向

## Non-goals

- 不修改 datasets/index.vue 的其他功能
- 不修改 images/index.vue 的功能
