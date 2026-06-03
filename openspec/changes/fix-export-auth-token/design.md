## Context

数据导出接口 `GET /api/v1/datasets/{id}/export` 需要 JWT 认证（`.anyRequest().authenticated()`）。前端 Token 存储在 `localStorage` 中，通过 Axios 拦截器自动注入到 JSON API 请求头。但 `handleExport()` 使用 `<a>` 标签触发下载，无法携带自定义请求头，导致发送的请求不含 `Authorization: Bearer <token>`，被 Spring Security `Http403ForbiddenEntryPoint` 拦截。

```
当前流：
  <a> tag ──→ GET /api/v1/datasets/12/export?format=geojson
               ↓
          无 Authorization header ──→ 403

目标流：
  fetch() ──→ GET /api/v1/datasets/12/export?format=geojson
               ↓
          Authorization: Bearer <token> ──→ 200 OK + 文件流
```

## Goals / Non-Goals

**Goals:**
- `handleExport()` 发送带 JWT Token 的 GET 请求到导出接口
- 后端响应正确流式返回文件内容
- 前端将响应内容作为文件下载（保留原文件名）

**Non-Goals:**
- 不改动后端 SecurityConfig / ExportController
- 不引入新的前端依赖
- 不修改其他页面的下载逻辑
- 不对超大文件做分块流式处理

## Decisions

### Decision 1：Fetch + Blob 方案

使用 `fetch()` 替代 `<a>` 标签，从 `localStorage` 读取 Token 并添加 `Authorization` 请求头。

```
fetch(url, {
  headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` }
})
  .then(res ⇒ res.blob())
  .then(blob ⇒ {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'filename.ext'
    a.click()
    URL.revokeObjectURL(url)
  })
```

**理由：**
- 唯一能携带 JWT 头的同时保持浏览器原生下载体验的方案
- 无需后端改动
- 与项目中 `MapContainer.vue` 中已有 Fetch + Token 的模式一致

### 被否定的方案

| 方案 | 问题 |
|---|---|
| Query param token（`?token=xxx`） | Token 暴露在浏览器历史、服务器日志、Referer 头中 |
| 后端添加 permitAll + IP 白名单 | 绕过认证不安全 |
| Axios + blob responseType | 与 Fetch 等价，但 React 生态倾向 Fetch；项目中已有 Fetch 用法 |
| `<a>` 标签 + Cookie Session | 需要后端改造为 session 认证，与现有 JWT 架构冲突 |

## Risks / Trade-offs

- **[低风险] 大文件内存占用**：blob 整体载入内存。GIS 数据集导出通常为 MB 级（GeoJSON/KML/Shapefile），远低于浏览器 2GB blob 上限。若未来出现百 MB 级导出，可改用 `Response.body.getReader()` 流式写入或 StreamSaver.js。
- **[低风险] Token 过期**：`handleExport` 执行时未检查 Token 有效期。199 (Token 过期) 由后端的 JwtAuthenticationFilter 处理，前端可增加错误提示。
