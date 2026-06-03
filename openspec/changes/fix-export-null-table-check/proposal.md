## Why

DataSetServiceImpl 中三处导出方法（`getDatasetAsGeoJSON`、`getDatasetAsKML`、`exportShapefileAsZip`）在检测到 `table_name` 为 null 时使用 `throw new RuntimeException("数据集无关联表")` 抛出异常。`RuntimeException` 会被 Spring Boot 默认处理为 500 错误，但已存的 `GlobalExceptionHandler` 不处理该类型，前端收到的错误信息不够友好。项目已有 `BusinessException` 类和对应的全局异常处理器，应将导出校验异常统一为 `BusinessException`，返回正确的 HTTP 状态码和清晰的业务提示。

## What Changes

1. 在 `DatasetServiceImpl.java` 中，将 3 处 `throw new RuntimeException("数据集无关联表")` 替换为 `throw BusinessException.badRequest("该数据集未导入空间数据，无法导出")`
2. 在 `DatasetServiceImpl.java` 中，将 3 处 `throw new RuntimeException("数据集不存在")` 替换为 `throw BusinessException.notFound("数据集不存在")`
3. 添加 `BusinessException` import

## 非目标

- 不修改 ExportController
- 不修改 GlobalExceptionHandler（已正确处理 BusinessException）
- 不修改 BusinessException 类本身
- 不涉及前端代码

## Capabilities

### New Capabilities
- `fix-export-null-table-check`: 导出时空表校验使用 BusinessException 返回清晰业务错误

### Modified Capabilities

无

## Impact

- `backend/src/main/java/com/gisplatform/service/impl/DatasetServiceImpl.java`：替换 RuntimeException 为 BusinessException
