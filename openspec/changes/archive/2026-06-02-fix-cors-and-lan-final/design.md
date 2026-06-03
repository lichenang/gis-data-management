## Context

当前存在局域网访问问题，具体表现为：
1. 前端通过局域网 IP 访问时，OPTIONS 预检请求返回 403
2. 前端请求后端地址硬编码为 `localhost:8088`，局域网设备无法连接

## Goals / Non-Goals

**Goals:**
- 修复 CORS 配置，确保 OPTIONS 预检请求被正确处理
- 前端通过相对路径或 Vite 代理请求后端，兼容局域网访问
- 确认 Vite 开发服务器允许局域网访问

**Non-Goals:**
- 不修改认证逻辑
- 不修改生产环境配置

## Decisions

### Decision 1: CORS 配置修复

**方案**: 在 SecurityConfig 中确保：
1. `.cors()` 已正确配置：`http.cors().configurationSource(corsConfigurationSource())`
2. allowedOrigins 包含 `http://192.168.31.123:3000`
3. allowedMethods 包含 OPTIONS（已包含）

### Decision 2: 前端 API 地址

**方案**: 修改 request.ts 中的 baseURL：
- 原：`baseURL: 'http://localhost:8088'`
- 改：`baseURL: '/api'`

**理由**:
- 使用 Vite 代理，开发环境前端请求会被代理到后端
- 相对路径方式不依赖硬编码地址
- 前端代码更简洁

### Decision 3: Vite 配置确认

**确认项**:
- `server.host: '0.0.0.0'` - 已配置
- `server.port: 3000` - 已配置
- `proxy['/api']` - 已配置

## Risks / Trade-offs

- **[低] 代理仅开发环境有效** — 生产环境需要 Nginx 配置反向代理
- **[低] CORS 配置安全性** — 开发环境允许更多来源，生产环境应使用严格配置

## Migration Plan

1. 验证/修复 SecurityConfig.java CORS 配置
2. 修改 request.ts baseURL
3. 验证 Vite 配置（如有需要）
4. 测试局域网访问

无数据库变更。

## Open Questions

- 是否需要为生产环境添加 Nginx 配置？
