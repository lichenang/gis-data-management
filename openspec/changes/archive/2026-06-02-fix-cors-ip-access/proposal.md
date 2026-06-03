## Why

当前后端 CORS 配置只允许 `localhost:3000` 访问，当开发者通过内网 IP `192.168.31.123:3000` 访问前端时，登录请求被浏览器 CORS 策略拦截，导致无法登录系统。

## What Changes

1. **修改允许的跨域源** — 在 CorsConfig 或 SecurityConfig 中添加 `http://192.168.31.123:3000` 到 allowedOrigins
2. **支持环境变量配置** — 通过环境变量配置 CORS 允许的来源，便于不同开发环境切换
3. **开发模式配置** — 考虑添加开发模式下允许所有源 (`allowedOrigins: "*"`) 的选项

## Capabilities

### New Capabilities
（本次变更为配置修改，不引入新能力）

### Modified Capabilities
（本次变更不修改已有规范的接口/能力行为）

## Impact

- `backend/.../config/CorsConfig.java` 或 `backend/.../config/SecurityConfig.java` — CORS 配置修改
- 不涉及 API 签名变更、不涉及数据库结构变更、不涉及前端代码变更

## Non-goals

- 不修改生产环境的 CORS 配置
- 不添加认证相关的功能
- 不涉及 UI/UX 变化

## Affected Files

- `backend/src/main/java/com/gisplatform/config/CorsConfig.java`（如存在）
- `backend/src/main/java/com/gisplatform/config/SecurityConfig.java`
