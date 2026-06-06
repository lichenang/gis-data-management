## Why

当 Shapefile 缺少 .prj 文件时，后端通过 `crsDetected=false` 返回给前端。但前端的源坐标系字段（sourceSrs）目前非必填，用户可以不选择源坐标系直接提交导入，导致后端无法正确转换坐标。

## What Changes

1. **前端表单验证增强**：当 `crsDetected=false` 时，`sourceSrs` 字段设为必填
2. **导入按钮禁用逻辑**：未选择源坐标系时，禁用"导入并创建"按钮

## Capabilities

### Modified Capabilities

- `conditional-srs-selector`: 增加 sourceSrs 字段的动态必填验证

## Impact

### 受影响的文件

**前端:**
- `frontend/src/views/datasets/index.vue`

### 非目标

- 不修改后端逻辑
- 不修改 crsDetected 的检测逻辑（已在 conditional-srs-selector 中实现）
