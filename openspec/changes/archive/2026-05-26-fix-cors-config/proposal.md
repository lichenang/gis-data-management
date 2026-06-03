# fix-cors-config

## Why

前端访问后端 API 时出现 403 错误，原因是 CORS（跨域资源共享）配置不正确。

当前环境：
- 前端地址：http://localhost:5173
- 后端地址：http://localhost:8088
- 浏览器阻止跨域请求

## What Changes

修改 `SecurityConfig.java` 中的 CORS 配置：

1. 使用 `setAllowedOrigins` 替代 `setAllowedOriginPatterns`，明确允许前端来源
2. 添加前端地址到允许的来源列表
3. 确保 OPTIONS 预检请求正确处理

## Capabilities

### Fixed Capabilities
- 前端跨域 API 调用

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/config/SecurityConfig.java`
- 影响：所有前端 API 请求能正常跨域访问

## Non-goals

- 不修改其他安全配置
- 不修改认证相关配置
