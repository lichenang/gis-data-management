## Context

当前项目已有认证基础设施但缺少 API 层：
- JwtTokenService - Token 生成/验证工具
- CustomUserDetailsService - 用户信息加载
- JwtAuthenticationFilter - Token 验证过滤器
- SecurityConfig - 安全配置（已放行 /api/v1/auth/**）

缺少：
- AuthController - 接收 HTTP 请求
- AuthService - 业务逻辑

## Goals / Non-Goals

**Goals:**
- 创建 AuthController 处理所有认证请求
- 创建 AuthService 封装登录逻辑
- 确保响应格式符合统一规范

**Non-goals:**
- 不实现用户创建后直接登录（先注册再登录）
- 不实现复杂权限验证

## Decisions

**API 响应格式统一：**
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**登录流程：**
1. 接收 username/password
2. 调用 CustomUserDetailsService 验证用户
3. 调用 JwtTokenService 生成 Token
4. 返回 Token 和过期时间

## Risk

无重大风险
