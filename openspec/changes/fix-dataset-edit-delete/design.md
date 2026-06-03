## Context

当前数据管理功能存在的具体问题：
1. **矢量编辑类型字段**：前端编辑表单未禁用类型字段，用户可修改 dataset.type
2. **上传状态提示**：上传区域无"已上传"状态反馈
3. **影像查看**：点击"查看"按钮仅跳转到地图或显示简单文本
4. **删除失败**：HTTP 403 或 404 错误，可能原因包括未传 ID 或 SecurityConfig 未放行

## Goals / Non-Goals

**Goals:**
- 修复矢量编辑：禁用类型字段、显示上传状态
- 修复影像查看：弹窗展示元数据
- 修复删除功能：确保 API 调用和后端配置正确

**Non-goals:**
- 不修改其他功能
- 不修改数据库结构

## Decisions

### Decision 1: 类型字段禁用

**选择**: 使用 HTML disabled 属性 + CSS 样式

**理由**:
- 简单直接，禁用后不可编辑
- 使用 CSS 保持视觉一致性

### Decision 2: 上传状态显示

**选择**: 已有文件时显示"已上传: {filename}"

**理由**:
- 用户可清晰看到已上传的文件
- 不需要重新上传时可直接保存

### Decision 3: 影像元数据弹窗

**选择**: 使用 Element Plus Dialog 组件

**理由**:
- Element Plus 内置组件，易于集成
- 可展示表格形式元数据

### Decision 4: 删除 API 检查

**选择**: 检查前端调用路径和后端 DELETE 映射

**理由**:
- 定位问题根源后针对性修复

## Implementation Details

### 前端修改
- 矢量编辑页面：类型 select 添加 disabled
- 矢量编辑页面：文件上传区域添加上传状态判断
- 影像查看：添加 Dialog 弹窗，展示 raster_metadata 表字段
- 删除按钮：检查 API 调用参数

### 后端修改
- SecurityConfig：添加 `/api/v1/dataset/**` 的 DELETE 方法放行
- 检查 DatasetController 是否有 @DeleteMapping 端点

## Risks / Trade-offs

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 类型字段禁用后无法恢复 | 数据类型确实不应修改 | 保持禁用是正确的 |
| 弹窗内容过多 | 元数据字段较多 | 使用表格布局，滚动查看 |
