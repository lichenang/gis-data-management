## Why

ExportController 及其调用的 DatasetServiceImpl 在业务异常时使用 `response.sendError(500, msg)` 返回错误。`sendError()` 不会直接写入响应，而是触发 Servlet 容器内部转发到 `/error` 端点。由于 Spring Security 配置了 `.anyRequest().authenticated()` 且 `/error` 不在 `permitAll` 列表中，该转发被拦截返回 403，掩盖了真实的错误信息（如"数据集无关联表"）。同时 ExportController 的部分校验（如 dataset 不存在返回 404）也使用 `response.setStatus()` 不做 body 写入，不符合项目统一的 `{ code, message, data }` 响应格式。

## What Changes

1. 在 `SecurityConfig` 中将 `/error` 路径加入 `permitAll()` 作为兜底
2. 修改 `ExportController`，将 `response.sendError()` 替换为直接写入 JSON 响应体，遵循 `{ code, message, data }` 格式
3. 修改 ExportController 中 `dataset == null` 的 404 处理，写入 JSON body 而非仅 `setStatus(404)`
4. 删除 `ExportController` 中 `catch` 块内的空 `try-catch` 嵌套，直接处理异常响应

## 非目标

- 不改动其他 Controller 的错误处理方式（本次仅修复 ExportController）
- 不修改 Service 层的异常抛出逻辑
- 不引入全局异常处理器（@ControllerAdvice），留待统一重构
- 不修改前端代码

## Capabilities

### New Capabilities
- `fix-export-error-masking`: 导出接口错误响应直接写入 JSON，避免 sendError → /error → 403 链路

### Modified Capabilities

无

## Impact

- `backend/src/main/java/com/gisplatform/config/SecurityConfig.java`：添加 `/error` 到 permitAll
- `backend/src/main/java/com/gisplatform/controller/ExportController.java`：替换所有 sendError/setStatus-only 为 JSON 响应写入
