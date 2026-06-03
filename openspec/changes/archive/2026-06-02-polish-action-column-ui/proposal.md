## Why

当前矢量数据集列表和影像管理列表的操作列 UI 不一致且功能分散。用户需要在一个紧凑的空间内完成数据导出/下载操作，当前列面存在多个下载按钮导致视觉混乱，也不符合常见的数据管理界面设计模式。

## What Changes

1. **矢量数据集列表 (datasets/index.vue)**
   - 导出功能已使用 el-dropdown 实现（无需修改）
   - 调整操作按钮颜色：编辑（primary）、发布/取消发布（success）、导出（warning）、删除（danger）
   - 确保按钮间距一致、操作列宽度固定、按钮水平居中对齐

2. **影像管理列表 (images/index.vue)**
   - 将"下载原始影像"、"下载切片包"、"下载元数据"三个按钮合并为"下载"下拉按钮（el-dropdown）
   - 调整操作按钮颜色与矢量管理一致
   - 保持操作列布局风格统一

3. **两个页面统一规范**
   - 操作列宽度保持一致（固定宽度）
   - 按钮在行内居中对齐
   - 按钮间距统一

## Capabilities

### New Capabilities
- 无新增能力，只是 UI 优化

### Modified Capabilities
- 无（纯前端样式调整，不涉及功能变更）

## Impact

- `frontend/src/views/datasets/index.vue` — 调整操作按钮样式和颜色
- `frontend/src/views/images/index.vue` — 合并下载按钮为下拉菜单，调整样式

## Non-goals

- 不修改后端 API
- 不改变现有功能和交互逻辑
- 不添加新的数据导出/下载格式

## Affected Files

- `frontend/src/views/datasets/index.vue`
- `frontend/src/views/images/index.vue`
