## Why

`GET /api/v1/datasets/{id}/export` 接口存在两个问题：

1. 当 `format` 参数为空时（`?format=`），直接返回 `400: "不支持的导出格式: "`，而非默认使用一种格式
2. `catch (Exception e)` 将所有异常包装为 500，掩盖了 Service 层已抛出的 `BusinessException`（如 400 的"该数据集未导入空间数据，无法导出"实际返回 500）

前者影响用户体验（前端易漏传 format），后者让之前 Service 层的修复无法生效。

## What Changes

1. `ExportController.exportDataset()` 中，当 `format` 参数为空字符串时，默认使用 `"geojson"`
2. `ExportController.exportDataset()` 的 catch 块中，区分 `BusinessException` 与普通异常，保持其原始错误码
3. 可选：在 Controller 调用 Service 前增加 `table_name` 预检查，提前返回 400（作为防御性编程）

## 非目标

- 不修改 `DatasetServiceImpl`（已在 `fix-export-null-table-check` 中完成）
- 不修改 `GlobalExceptionHandler`
- 不涉及前端代码或 R 响应格式的统一

## Capabilities

### New Capabilities
- `export-robustness`: 导出接口的健壮性处理，包括空 format 默认值和合理的异常状态码

### Modified Capabilities

无

## Impact

- `backend/src/main/java/com/gisplatform/controller/ExportController.java`：修改 `exportDataset` 方法的 format 处理和 catch 块
