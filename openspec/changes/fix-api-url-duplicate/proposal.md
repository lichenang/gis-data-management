# fix-api-url-duplicate

## Why

前端登录请求地址为 `/api/v1/v1/auth/login`，多了一个 `/v1` 片段，导致 404 错误。

问题原因：
- `request.ts` 中 baseURL 包含 `/api/v1`
- `store/user.ts` 中 API 路径又包含 `/v1`
- 拼接后变成 `/api/v1/v1/auth/login`

## What Changes

修改 `frontend/src/store/user.ts` 中三处 API 路径，移除多余的 `/v1` 前缀：
- `/v1/auth/login` → `/auth/login`
- `/v1/auth/logout` → `/auth/logout`
- `/v1/auth/userinfo` → `/auth/userinfo`

## Capabilities

### Fixed Capabilities
- 前端 API 调用

## Impact

- 修改文件: `frontend/src/store/user.ts`
- 影响: 所有 API 请求路径正确

## Non-goals

- 不修改 request.ts 中的 baseURL 配置
- 不修改后端 API 路径
