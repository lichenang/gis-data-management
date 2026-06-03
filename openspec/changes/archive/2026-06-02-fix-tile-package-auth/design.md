## Context

前端 Axios 实例 `service` 在 `request.ts` 中配置了统一的请求/响应拦截器。请求拦截器自动注入 JWT Token（正常工作），但响应拦截器对所有响应按 JSON 格式解析（`const { code, message } = response.data`）。

切片包下载接口 `POST /api/v1/images/{id}/tile-package` 使用 `responseType: 'blob'`，返回二进制 ZIP 流。此时 `response.data` 是 Blob 对象，解构得到 `{ code: undefined, message: undefined }`，拦截器判断 `code !== 200` 后弹出"请求失败"并 `Promise.reject`——导致浏览器永远不会触发下载。

**请求/响应拦截器执行流程（当前）：**
```
请求 (JWT Token ✅)
  → Axios 发送
    → 后端返回 200 + ZIP 二进制
      → 响应拦截器: response.data = Blob
        → 解构: { code: undefined, message: undefined }
          → code !== 200 → ElMessage.error('请求失败')
            → Promise.reject() ❌ 下载中断
```

## Goals / Non-Goals

**Goals:**
- 修复响应拦截器对 `responseType: 'blob'` 请求的处理，跳过 JSON 解析，直接返回 Blob
- 确保 2xx 状态码下浏览器正常触发 ZIP 文件下载
- 确保 4xx/5xx 错误时能显示后端返回的正确错误消息

**Non-Goals:**
- 不修改后端代码
- 不修改 `api/image.ts` 中的 `downloadTilePackage` 函数
- 不改变其他 API 的 JSON 解析行为

## Decisions

### 1. 检测策略：检查 `response.config.responseType`

| 方案 | 描述 | 评价 |
|------|------|------|
| A. 检查 `response.config.responseType === 'blob'` | 根据请求配置判断 | ✅ 精确，只影响 blob 请求 |
| B. 检查 `response.data instanceof Blob` | 运行时类型判断 | ❌ 后端错误时 Blob 可能混入 JSON 文本 |
| C. 新增单独 axios 实例 | 为 blob 请求创建独立实例 | ❌ 代码重复，需要单独配置 JWT 拦截器 |

**结论**：选择方案 A。`response.config.responseType` 在 Axios 响应中始终可用，能精确区分 blob 请求和其他请求。当检测到 `responseType === 'blob'` 时，直接 `return response.data` 跳过 JSON 解析逻辑。

### 2. 错误处理

当 `responseType: 'blob'` 且后端返回非 2xx 时，Axios 会将错误体也包装为 Blob。此时需要特殊处理：

- 在响应拦截器的 success handler 中检查 `response.status` 是否 2xx（虽然按 Axios 设计 2xx 才会进入 success handler）
- 在 error handler 中读取错误体的文本内容（将 Blob 转为文本），提取错误消息

但考虑到切片包下载的 timeout 为 300 秒，后端一般不会返回 2xx 错误，现有的 error handler 也能满足基本需求。

### 3. 不影响其他 API

修复仅在 `response.config.responseType === 'blob'` 条件满足时生效，其他 API 的 JSON 解析行为不受影响。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| [低] 其他 API 也用了 `responseType: 'blob'` 但需 JSON 解析 | 项目中无此情况；即使有，二进制场景跳过 JSON 解析也是正确行为 |
| [低] 后端 blob 响应返回 200 但内部 `code != 200` | 对于二进制下载接口，HTTP 200 即表示成功，无需二次校验 |
| [中] 后端 blob 错误响应（如 500）的错误消息无法展示 | 目前 `downloadTilePackage` 使用 `responseType: 'blob'` 时，后端异常会写 JSON 到响应体但 Content-Type 是 `application/zip`；后续可优化 controller 在失败时返回 JSON |
