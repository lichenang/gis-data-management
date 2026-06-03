## Context

当前前端代码存在以下问题：
1. `vite.config.ts` 未配置 `server.host`，Vite 默认只监听 `localhost`，局域网设备无法访问
2. `src/api/request.ts` 中 baseURL 硬编码为 `http://localhost:8088`，局域网环境下无法连接到后端

这些问题导致开发者无法在手机、平板等设备上测试前端应用。

## Goals / Non-Goals

**Goals:**
- 允许局域网设备访问 Vite 开发服务器
- 前端能自动适配当前访问地址，请求正确的后端服务
- 使用 Vite 代理简化开发环境配置，避免跨域问题

**Non-Goals:**
- 不修改生产环境配置
- 不修改后端代码

## Decisions

### Decision 1: Vite 服务器配置

**方案**: 在 `vite.config.ts` 中添加 `server.host: '0.0.0.0'` 和 `server.port: 3000`

**理由**:
- `0.0.0.0` 允许所有网络接口监听，局域网设备可以通过本机 IP 访问
- 显式指定端口避免随机分配

### Decision 2: API 请求地址

**方案**: 使用 Vite 代理，将 `/api` 请求代理到后端 `http://localhost:8088`

配置：
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    }
  }
}
```

**理由**:
- 代理方案最简单，前端代码无需修改
- 解决开发环境跨域问题（后端 CORS 配置可以保持更严格）
- 前端请求 `/api/xxx` 会被代理到 `http://localhost:8088/api/xxx`

**备选**: 修改 request.ts 动态获取 baseURL — 需要处理更多边界情况，且不如代理方案简洁。

## Risks / Trade-offs

- **[低] 代理仅开发环境有效** — 生产环境需要 Nginx 反向代理，这是预期的
- **[低] 需要后端 CORS 支持 localhost** — 开发环境浏览器会先发送预检请求到前端，再由前端代理到后端

## Migration Plan

1. 修改 `vite.config.ts` 添加服务器配置和代理
2. 测试通过局域网 IP 访问前端
3. 验证 API 请求正常工作

无数据库变更，无需回滚。

## Open Questions

- 是否需要添加 `.env.development` 配置文件支持不同后端地址？
