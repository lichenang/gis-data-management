## Why

访问 POST /api/v1/auth/login 返回 500 错误，提示 "No static resource api/v1/auth/login"。原因是项目中缺少 AuthController 来处理认证请求，但 SecurityConfig 已配置该路径需要放行。

## What Changes

- 创建 AuthController 处理认证 API 请求
  - POST /api/v1/auth/login - 用户登录
  - POST /api/v1/auth/register - 用户注册
  - POST /api/v1/auth/refresh - 刷新 Token
  - GET /api/v1/auth/info - 获取当前用户信息

- 创建 AuthService 封装认证逻辑
  - 用户名密码验证
  - JWT Token 生成
  - 用户信息返回

- 登录接口请求格式：{ "username": "xxx", "password": "xxx" }
- 响应格式统一：{ "code": 200, "message": "success", "data": { ... } }

## Capabilities

### New Capabilities
- `auth-api`: 认证接口 API

### Modified Capabilities
- 无

## Impact

- 新增文件：
  - `backend/src/main/java/com/gisplatform/controller/AuthController.java`
  - `backend/src/main/java/com/gisplatform/service/AuthService.java`
  - `backend/src/main/java/com/gisplatform/service/impl/AuthServiceImpl.java`
  - `backend/src/main/java/com/gisplatform/dto/LoginRequest.java`
  - `backend/src/main/java/com/gisplatform/dto/RegisterRequest.java`

## Non-goals

- 不实现完整的用户管理功能
- 不修改 SecurityConfig
- 不实现登出功能（后续迭代）
