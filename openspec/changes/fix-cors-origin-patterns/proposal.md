# fix-cors-origin-patterns

## Why

前端登录请求返回 403 错误，OPTIONS 预检请求也被拒绝。

问题根因：
- 前端 Vite 开发服务器端口：3000
- 后端 CORS 配置允许的端口：5173（错误）
- 请求 Origin: `http://localhost:3000` 不在允许列表中，导致 CORS 检查失败

## What Changes

修改 `SecurityConfig.java` 的 CORS 配置：
- 使用 `setAllowedOriginPatterns` 替代 `setAllowedOrigins`
- 允许 `http://localhost:*` 和 `http://127.0.0.1:*` 所有本地端口

## Capabilities

### Fixed Capabilities
- 前端跨域访问所有 API
- 支持多端口开发（3000、5173 等）

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/config/SecurityConfig.java`
- 影响：前端可以正常登录和调用 API

## Non-goals

- 不修改权限配置
- 不修改其他安全设置
