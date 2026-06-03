## Why

局域网设备访问前端时，存在两个阻断性问题：
1. 后端 Spring Security 的 CORS 配置中，OPTIONS 预检请求可能被拦截返回 403，导致无法完成跨域请求
2. 前端 API 请求地址硬编码为 `localhost:8088`，局域网设备无法连接

## What Changes

1. **修复 CORS 配置** — 检查并修复 SecurityConfig.java，确保：
   - `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` 正确集成
   - 局域网地址 `http://192.168.31.123:3000` 在 allowedOrigins 中
   - allowedMethods 包含 OPTIONS

2. **修复前端 API 请求地址** — 修改 `src/api/request.ts` 中的 baseURL：
   - 将 `http://localhost:8088` 改为相对路径 `/api`

3. **确认 Vite 配置** — 确认 vite.config.ts 中：
   - `server.host: '0.0.0.0'` 允许局域网访问
   - `/api` 代理指向 `http://localhost:8088`

## Capabilities

### New Capabilities
（本次变更为配置修复，不引入新能力）

### Modified Capabilities
（本次变更不修改已有规范的接口/能力行为）

## Impact

- `backend/.../config/SecurityConfig.java` — CORS 配置修复
- `frontend/src/api/request.ts` — 请求地址修改
- `frontend/vite.config.ts` — 验证配置正确
- 不涉及 API 签名变更、不涉及数据库结构变更

## Non-goals

- 不修改认证逻辑
- 不修改生产环境配置
- 不添加新的 UI 功能

## Affected Files

- `backend/src/main/java/com/gisplatform/config/SecurityConfig.java`
- `frontend/src/api/request.ts`
- `frontend/vite.config.ts`
