## Why

数据导出的前端 `handleExport()` 函数使用 `<a>` 标签触发下载（`link.href = url`），这种方式发送的是原生 GET 请求，**无法携带 JWT Authorization 请求头**。后端 Spring Security 配置了 `.anyRequest().authenticated()`，未携带 Token 的请求被 `Http403ForbiddenEntryPoint` 拒绝，导致导出功能 403。而 Token 已存储在 localStorage 中（由 Axios 拦截器自动注入到 JSON API 请求中），只需让下载请求也能携带 Token 即可。

## What Changes

1. 将 `datasets/index.vue` 中的 `handleExport()` 从 `<a>` 标签触发下载改为 **Fetch + Blob** 方案
2. Fetch 请求从 `localStorage` 读取 `access_token`，设置 `Authorization: Bearer <token>` 请求头
3. 使用 `URL.createObjectURL(blob)` 创建临时下载链接
4. 后端无需改动

## 非目标

- 不改动后端 Spring Security 配置
- 不涉及后端 ExportController 的任何修改
- 不涉及其他数据集的下载方式（仅影响矢量/栅格导出）
- 不添加文件流式分块下载（未来优化）

## Capabilities

### New Capabilities
- `fix-export-auth-token`: 数据导出请求携带 JWT Token 认证

### Modified Capabilities

无

## Impact

- `frontend/src/views/datasets/index.vue`：重写 `handleExport()` 函数
