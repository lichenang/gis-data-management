## Context

当前 `packageTiles()` 在 `ZipOutputStream`（try-with-resources）内部检查 `totalWritten == 0` 并抛出异常。try-with-resources 在抛出异常前先调用 `zos.close()`，向输出流写入 ZIP 中央目录（约 22 字节），导致 response 进入"已部分提交"状态。此时 Controller 的 `writeErrorResponse()` 即使调用 `response.reset()`，在某些 Servlet 容器中也可能因响应已提交而无效，最终浏览器收到损坏的空 ZIP。

## Goals / Non-Goals

**Goals:**
- 在 `ZipOutputStream` 创建之前预检查瓦片文件是否存在，提前失败（此时 response 未写入任何字节）
- 移除后置 `totalWritten == 0` 检查及相关的 IOException catch 块中的空分支
- 简化 Controller，移除自行处理异常的代码，让异常统一由 `GlobalExceptionHandler` 处理

**Non-Goals:**
- 不修改 `GlobalExceptionHandler`
- 不修改瓦片坐标计算逻辑
- 不修改 API 请求/响应格式

## Decisions

### 决策 1：预检查使用枚举共享方法

**方案**：提取 `enumerateTileFiles()` 方法，返回匹配的瓦片文件列表。`packageTiles()` 先调用该方法获取列表，空则提前抛异常；非空则遍历列表写入 ZIP。

**替代方案**：目录扫描（`Files.walk` 查找 .png）。该方案不够精确——可能扫描到不属于当前 zoom/bounds 范围的瓦片，也可能漏掉深层子目录。枚举方法精确匹配三层循环逻辑。

**权衡**：预枚举会将所有匹配的瓦片文件路径装载到内存列表中。对于大范围下载（百万级别），内存开销显著。但对于有 `maxTiles` 限制（通常数万）的场景，集合大小可控。

### 决策 2：Controller 直接 throw，由全局异常处理器接管

**方案**：移除 Controller 的 `try-catch` 块，让异常自然传播。`GlobalExceptionHandler` 的 `@ExceptionHandler(RuntimeException.class)` 返回 `R.fail("系统繁忙，请稍后再试")`。

**可行性**：由于预检查确保异常在 `response.getOutputStream()` 调用之前抛出，response 处于全新状态，`GlobalExceptionHandler` 可通过 Spring MVC 正常写入 JSON 错误响应。不再有 `getOutputStream()`/`getWriter()` 冲突问题。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 预枚举列表内存占用 | 大范围瓦片场景可能 OOM | `maxTiles` 限制已在瓦片数估算后拦截（第 75-78 行），有效控制列表大小 |
| `enumerateTileFiles()` 与 ZIP 写入的解耦度 | 若后续需流式写入大文件，枚举模式不适用 | 当前 `maxTiles` 限制下枚举模式足够；未来可改为迭代器/回调模式 |
| 全局异常处理器返回的消息不包含具体错误详情 | 用户收到统一"系统繁忙"而非具体错误 | GlobalExceptionHandler 已在日志中记录 `e.getMessage()`；若需自定义错误消息可在服务层异常中包含描述性消息 |
