## Context

导出接口 `GET /api/v1/datasets/{id}/export` 在业务异常时（如数据集无关联表）进入 catch 块调用 `response.sendError(500, msg)`。该方法不直接写入响应体，而是由 Servlet 容器内部 `RequestDispatcher.forward()` 到 `/error` 路径。Spring Security 的 `FilterChainProxy` 会重新处理该请求，但认证上下文在 forward 过程中丢失，且 `/error` 不在 `permitAll` 列表中，导致最终返回 403。

```
问题链路：
ExportController catch → sendError(500, "数据集无关联表")
                     → Servlet forward to /error
                     → SecurityContext 丢失 → anonymous
                     → Http403ForbiddenEntryPoint
                     → 客户端收到 403（而非 500）
```

## Goals / Non-Goals

**Goals:**
- 导出接口出错时返回真正的 HTTP 状态码和 JSON 错误体，不再被 403 掩盖
- 响应格式统一为 `{ code, message, data: null }`
- 添加 `/error` 到 `permitAll` 作为兜底安全网

**Non-Goals:**
- 不引入 `@ControllerAdvice` 全局异常处理器
- 不修改其他 Controller 的错误处理
- 不修改 Service 层

## Decisions

### Decision 1：直接写入 JSON 响应替代 sendError

```java
// 替换前
response.sendError(400, "不支持的导出格式: " + format);

// 替换后
response.setStatus(400);
response.setContentType("application/json;charset=UTF-8");
response.getWriter().write(
    "{\"code\":400,\"message\":\"不支持的导出格式: " + format + "\",\"data\":null}"
);
```

- **理由**：`sendError()` 的转发行为与 JWT 无状态认证架构冲突。直接写入响应体可以完全避免错误转发。
- **格式**：使用项目统一的 `{ code, message, data }` 约定。

### Decision 2：简化 catch 块

当前 catch 块：
```java
} catch (Exception e) {
    try {
        response.sendError(500, "导出失败: " + e.getMessage());
    } catch (Exception ignored) {
    }
}
```

嵌套 try-catch 是为了防止 `sendError` 在响应已提交时抛异常。改用直接写入后，可以消除嵌套：

```java
} catch (Exception e) {
    response.setStatus(500);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(
        "{\"code\":500,\"message\":\"导出失败: " + e.getMessage() + "\",\"data\":null}"
    );
}
```

### Decision 3：/error 路径加入 permitAll

作为兜底安全网，防止其他未覆盖的 `sendError` 调用同样被拦截：
```java
.requestMatchers("/error", ...).permitAll()
```

- **理由**：即使本次修复了 ExportController，其他 Controller 可能仍有 `sendError()` 调用未覆盖。

## Risks / Trade-offs

- **[低风险] 响应格式不一致**：`sendError()` 走 `/error` 由 Spring Boot 的 `BasicErrorController` 渲染 HTML/JSON。直接写入 JSON 后格式变为 `{ code, message, data }`，与项目其他 API 一致但不同于 Spring Boot 默认的 `{ error, message, status, path }` 格式。这是期望行为。
- **[低风险] 字符编码**：`response.getWriter()` 在写入前需确保 `Content-Type` 包含 `charset=UTF-8`。已通过 `setContentType("application/json;charset=UTF-8")` 保证。
