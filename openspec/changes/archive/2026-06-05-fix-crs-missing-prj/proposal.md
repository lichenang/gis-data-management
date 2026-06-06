## Why

当 Shapefile 缺少 .prj 文件时，GeoTools 无法读取坐标系信息（nativeCrs 为 null 或无法识别），导致 CrsTransformUtil.getEpsgCode() 返回 0。虽然代码中有 fallback 逻辑使用用户指定的 targetSrs，但这个 fallback 仅在 targetSrs 有效时使用，且没有正确传递给 insertFeature 进行坐标转换。

## What Changes

1. **后端修复**: 当 nativeCrs 为 null 或无法识别时（nativeSrid == 0），使用用户在导入表单中指定的 sourceSrs 参数作为源坐标系
2. **前端提示**: 在上传对话框中增加明确提示，告知用户如果 Shapefile 缺少 .prj 文件，必须手动指定原始投影

## Capabilities

### Modified Capabilities

- `china-crs-detection`: 增强坐标系处理逻辑，支持用户指定的 sourceSrs 作为 fallback
- `multi-format-vector-import`: 修改 importUsingDataStore 使用用户指定的 sourceSrs

## Impact

### 受影响的文件

**后端:**
- `backend/src/main/java/com/gisplatform/service/impl/MultiFormatImportServiceImpl.java`

**前端:**
- `frontend/src/views/datasets/index.vue` (上传对话框)

### 非目标

- 不修改 GeoTools 相关的 CRS 解析代码
- 不添加新的 API 接口
