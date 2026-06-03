## Why

当前前端请求后端地址硬编码为 `localhost:8088`，当用户在局域网内通过其他设备（如手机、平板）访问时，无法连接到后端服务，导致功能不可用。

## What Changes

1. **修改 Vite 开发服务器配置** — 在 `vite.config.ts` 中添加 `server.host: '0.0.0.0'` 允许局域网访问
2. **修改前端 API 请求地址** — 修改 `src/api/request.ts` 中的 baseURL，从硬编码 localhost 改为动态获取或使用相对路径
3. **配置 Vite 代理** — 使用 Vite 代理将 `/api` 请求代理到后端，解决跨域问题并简化配置

## Capabilities

### New Capabilities
（本次变更为配置修改，不引入新能力）

### Modified Capabilities
（本次变更不修改已有规范的接口/能力行为）

## Impact

- `frontend/vite.config.ts` — Vite 开发服务器配置
- `frontend/src/api/request.ts` — Axios 请求配置
- 不涉及 API 签名变更、不涉及数据库结构变更

## Non-goals

- 不修改生产环境部署配置
- 不修改认证流程
- 不添加新的 UI 功能

## Affected Files

- `frontend/vite.config.ts`
- `frontend/src/api/request.ts`
