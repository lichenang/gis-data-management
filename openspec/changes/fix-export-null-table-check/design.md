## Context

项目已有完整的异常处理体系：`BusinessException`（位于 `com.gisplatform.common.exception`）提供 `badRequest()`、`notFound()` 等工厂方法，`GlobalExceptionHandler` 统一捕获 `BusinessException` 并返回 `{ code, message, data }` 格式的 JSON 响应。然而，`DatasetServiceImpl` 中三处导出方法在检测到 `table_name` 为 null 时直接 `throw new RuntimeException`，导致 Spring Boot 默认返回 500 且错误信息不够友好。

## Goals / Non-Goals

**Goals:**
- 将 `DatasetServiceImpl` 中 3 处导出方法的 `RuntimeException` 替换为 `BusinessException`，使用合适的业务状态码和提示信息

**Non-Goals:**
- 不修改 `BusinessException`、`GlobalExceptionHandler` 或控制器层

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 异常类型 | `BusinessException.badRequest()` / `BusinessException.notFound()` | 项目已有，被全局异常处理器正确处理。`GlobalExceptionHandler` 使用 `@ResponseStatus(HttpStatus.OK)` + `R.fail(code, message)` 返回统一格式 |
| 错误码复用现有常量 | `BAD_REQUEST=400`、"该数据集未导入空间数据，无法导出" | 与项目已定义的异常码常量一致，用户体验统一 |

## Risks / Trade-offs

无风险。此变更仅替换异常类型，不改变业务逻辑流程。
