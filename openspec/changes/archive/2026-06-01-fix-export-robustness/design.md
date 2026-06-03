## Context

`ExportController.exportDataset()` 当前在 `catch (Exception e)` 中统一返回 500，这会覆盖 `DatasetServiceImpl` 中已抛出的 `BusinessException`（如 `badRequest(400)` 的"该数据集未导入空间数据，无法导出"）。同时，`format` 参数为空字符串时直接报错而非默认使用 GeoJSON。

## Goals / Non-Goals

**Goals:**
- 导出接口在 `format` 为空时默认使用 GeoJSON
- 导出时 `table_name` 为空的场景返回 400 而非 500
- 保持与 Service 层已实现的 `BusinessException` 一致

**Non-Goals:**
- 不改变 Controller 直接写 response 的模式（不与 `GlobalExceptionHandler` 集成）
- 不修改 `DatasetServiceImpl`、`GlobalExceptionHandler`、`BusinessException`

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| format 空值处理 | `if (format == null \|\| format.isEmpty()) format = "geojson"` | GeoJSON 是最通用的矢量格式，也是上游导入的默认格式。不改变 API 签名（仍为 `@RequestParam` 而非 `required=false`）以避免影响其他调用方 |
| table_name 预检查 | 在 Controller 中于调用 Service 前检查 `dataset.getTableName()`，为空则返回 400 | 防御性编程，避免 Service 层抛出 BusinessException 被 catch 块误处理 |
| BusinessException 处理 | catch 块中通过 `instanceof BusinessException` 区分，使用其 `getCode()` 和 `getMessage()` | 简单直接，无需修改异常处理架构 |

```
                    FIXED FLOW
                    ══════════════════════

   GET /api/v1/datasets/{id}/export?format=
          │
          ▼
   format is blank → default to "geojson"
          │
          ▼
   dataset = getById(id)
          │
          ├── null/deleted → 404 "数据集不存在" (return)
          │
          ▼
   dataset.getTableName()
          │
          ├── null/empty → 400 "该数据集未导入空间数据，无法导出" (return)
          │
          ▼
   try {
       datasetService.exportXxx()
   } catch (BusinessException e) {
       writeErrorJson(response, e.getCode(), e.getMessage())  ← 保持原始错误码
   } catch (Exception e) {
       writeErrorJson(response, 500, "导出失败: " + e.getMessage())
   }
```

## Risks / Trade-offs

无风险。此变更仅增强 Controller 的健壮性，不改变业务逻辑。
