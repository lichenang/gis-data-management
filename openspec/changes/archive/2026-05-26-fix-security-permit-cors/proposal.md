# fix-security-permit-cors

## Why

应用 `fix-cors-config` 变更后，所有 API 返回 403 错误。

根据诊断报告 `openspec/specs/debug-cors-403.md` 分析：
- 浏览器发送 CORS 预检请求使用 `OPTIONS` 方法
- 当前 SecurityConfig 只放行了特定路径（如 `/api/v1/auth/login`），但没有匹配 OPTIONS 方法
- OPTIONS 预检请求落入 `anyRequest().authenticated()` 被拒绝

## What Changes

1. 添加 `HttpMethod.OPTIONS` 的全局放行，在所有其他规则之前
2. 确保 CORS 配置不变，保持已有的明确来源配置

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/config/SecurityConfig.java`
- 影响：修复 CORS 预检请求 403 错误

## Non-goals

- 不修改已有的 permitAll 路径列表
- 不修改 CORS allowedOrigins 配置
