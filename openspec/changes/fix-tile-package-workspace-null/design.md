## Context

切片包下载接口中，Controller 在调用 `packageTiles()` 前已设置 `Content-Type: application/zip` 并调用 `response.getOutputStream()`，导致 response 进入已提交状态。此时若 Service 层抛出 `RuntimeException`（如“该影像尚未生成切片缓存”），`GlobalExceptionHandler` 尝试 `response.reset()` 及回写 JSON 均因 response 已提交而失败，客户端最终收到 0B 空 ZIP 或 Connection 异常。

当前 TilePackageServiceImpl 第 65 行使用 `geoServerProperties.getWorkspace()` 构造 GWC 目录名，默认值为 `"gisplatform"`，虽非直接根因，但在 workspace 为空串或 null 时缺少防御性 fallback。

## Goals / Non-Goals

**Goals:**
- 在 `GlobalExceptionHandler` 的 `RuntimeException` 和 `Exception` 处理器中，检测 `response.isCommitted()`，已提交时跳过 reset 和 JSON 写入，仅记录日志
- 在 `TilePackageServiceImpl` 中，对 `geoServerProperties.getWorkspace()` 添加 null/empty fallback 为 `"gisplatform"`
- 确保异常发生时客户端能收到有效的 JSON 错误响应（而非空 ZIP 或连接错误）

**Non-Goals:**
- 不修改 Controller 的流式响应结构（仍使用 `response.getOutputStream()` + `ZipOutputStream`）
- 不修改 `enumerateTileFiles` 或 tileY 公式
- 不修改 GeoServerProperties 默认值
- 不引入新依赖

## Decisions

### Decision 1: GlobalExceptionHandler — response.isCommitted() 保护

**方案**: 在 `handleRuntimeException` 和 `handleException` 方法中，先检查 `response.isCommitted()`：

```java
if (response.isCommitted()) {
    logger.warn("Response already committed, skipping error response: {} - {}", request.getRequestURI(), e.getMessage());
    return null;  // void return, Spring will not write body
}
```

**理由**:
- 当 response 已提交（如流式下载场景），任何写入操作都会抛出 `IllegalStateException`
- `response.reset()` 对已提交 response 调用同样会抛异常（即使 try-catch 也不能阻止后续 `@ResponseStatus` 尝试设状态码）
- 返回 `null` 让 Spring 跳过 body 写入，当前 try-catch 保留作为防御

**备选方案**: 使用 `response.getOutputStream().flush()` 后关闭流 — 但已提交流无法再写入，无效。

### Decision 2: TilePackageServiceImpl — workspace fallback

**方案**: 在构造 GWC 目录名前，添加 null/empty 保护：

```java
String workspace = geoServerProperties.getWorkspace();
if (workspace == null || workspace.isEmpty()) {
    workspace = "gisplatform";
}
```

**理由**:
- 当前 `GeoServerProperties.workspace` 默认值 `"gisplatform"`（见 `GeoServerProperties.java:18`），正常情况下不会为 null
- 但若用户通过环境变量 `GEOSERVER_WORKSPACE:` 显式设为空，`getWorkspace()` 返回空串
- 添加防御性 fallback 保持行为一致，避免路径变为 `_raster_38` 或 `null_raster_38`

## Risks / Trade-offs

- **[Low] GlobalExceptionHandler 返回 null**: 当 response 已提交时，客户端可能收到截断的 ZIP 流（而非 JSON 错误）。这是流式下载场景的固有限制。Mitigation: 确保 Service 层预检查（`enumerateTileFiles` 空检查）在 `getOutputStream()` 前抛出异常，避免已提交状态下的错误路径。
- **[Low] workspace fallback 隐藏配置错误**: 如果用户本意是使用空 workspace，fallback 会掩盖配置问题。Mitigation: 空 workspace 在正常业务中无意义，fallback 为默认值是合理行为。
