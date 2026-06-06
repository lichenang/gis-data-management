## Why

当前 Shapefile 上传时，前端始终显示"源坐标系"下拉框，但当 .prj 文件存在且 CRS 能被自动识别时，这个下拉框是多余的。用户需要的是：根据 .prj 文件是否存在来决定显示内容。

## What Changes

1. **后端 parseFile 增强**：解析 Shapefile 时检测 CRS 是否能自动识别，返回 `crsDetected` 字段
2. **前端条件显示**：根据 `crsDetected` 状态条件性地显示源坐标系选择器

## Capabilities

### Modified Capabilities

- `conditional-srs-selector`: 修改 parseFile API 和前端上传对话框的条件显示逻辑

## Impact

### 受影响的文件

**后端:**
- `backend/src/main/java/com/gisplatform/dto/GisDataParseResult.java`
- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

**前端:**
- `frontend/src/api/dataset.ts`
- `frontend/src/views/datasets/index.vue`

### 非目标

- 不修改导入时的 CRS 转换逻辑（已在 fix-crs-missing-prj 中实现）
- 不在前端 parse 阶段验证用户选择的 sourceSrs 是否有效
