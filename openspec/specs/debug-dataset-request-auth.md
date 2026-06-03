# 诊断报告: 前端请求 JWT Token 问题排查

## 问题描述

用户点击"仅创建"按钮时报错"系统繁忙，请稍后再试"

## 认证流程分析

### 完整调用链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        认证流程                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. 登录流程                                                         │
│     ┌────────────────────────────────────────────────────────────┐ │
│     │ POST /api/v1/auth/login                                    │ │
│     │     ↓                                                       │ │
│     │ 后端验证用户名密码，返回 accessToken                        │ │
│     │     ↓                                                       │ │
│     │ 前端: localStorage.setItem('access_token', accessToken)   │ │
│     └────────────────────────────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  2. 路由守卫 (router/index.ts)                                      │
│     ┌────────────────────────────────────────────────────────────┐ │
│     │ to.meta.requiresAuth === true                              │ │
│     │ userStore.token = localStorage.getItem('access_token')    │ │
│     │     ↓                                                       │ │
│     │ hasToken ? 放行 : 重定向到 /login                          │ │
│     └────────────────────────────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  3. 请求发送 (api/dataset.ts → api/request.ts)                     │
│     ┌────────────────────────────────────────────────────────────┐ │
│     │ service.interceptors.request.use()                        │ │
│     │     ↓                                                       │ │
│     │ token = localStorage.getItem('access_token')              │ │
│     │     ↓                                                       │ │
│     │ config.headers.Authorization = `Bearer ${token}`          │ │
│     └────────────────────────────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  4. 后端 Security (SecurityConfig.java)                            │
│     ┌────────────────────────────────────────────────────────────┐ │
│     │ .requestMatchers("/api/v1/auth/**").permitAll()           │ │
│     │ .anyRequest().authenticated()                             │ │
│     │     ↓                                                       │ │
│     │ /api/v1/datasets 需要认证！不在 permitAll 列表中           │ │
│     └────────────────────────────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  5. JWT 过滤器 (JwtAuthenticationFilter.java)                      │
│     ┌────────────────────────────────────────────────────────────┐ │
│     │ jwt = getJwtFromRequest(request)  // 从 Header 获取        │ │
│     │     ↓                                                       │ │
│     │ jwtTokenService.validateToken(jwt)  // 验证 Token          │ │
│     │     ↓                                                       │ │
│     │ userId = jwtTokenService.getUserIdFromToken(jwt)          │ │
│     │     ↓                                                       │ │
│     │ authentication = new UsernamePasswordAuthenticationToken  │ │
│     │   (username, userId, authorities)                          │ │
│     │     ↓                                                       │ │
│     │ SecurityContextHolder.getContext().setAuthentication()   │ │
│     └────────────────────────────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  6. Controller & Service                                           │
│     ┌────────────────────────────────────────────────────────────┐ │
│     │ DatasetController.create()                                 │ │
│     │     ↓                                                       │ │
│     │ DatasetServiceImpl.createDataset()                         │ │
│     │     ↓                                                       │ │
│     │ currentUserUtils.getCurrentUserId()                        │ │
│     │     ↓                                                       │ │
│     │ SecurityContextHolder.getContext().getAuthentication()    │ │
│     │     ↓                                                       │ │
│     │ auth.getCredentials() → userId                             │ │
│     └────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 关键配置检查

### 1. 前端请求拦截器 (request.ts)

```typescript
// 第 25-40 行
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`  // ✅ 正确添加 Token
    }
    // ...
    return config
  }
)
```

**状态**: ✅ 正确配置，从 localStorage 读取 `access_token` 并添加到请求头

---

### 2. 后端 Security 配置 (SecurityConfig.java)

```java
// 第 111-130 行
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers(
            "/api/v1/auth/login",
            "/api/v1/auth/register", 
            "/api/v1/auth/refresh",
            // ... 其他公开接口
    ).permitAll()
    .anyRequest().authenticated()  // ← 关键：其他所有请求需要认证
)
```

**状态**: ✅ `/api/v1/datasets` 不在 permitAll 列表，需要认证

---

### 3. 前端路由守卫 (router/index.ts)

```typescript
// 第 55-66 行
const requiresAuth = to.meta.requiresAuth as boolean
const hasToken = !!userStore.token

if (requiresAuth && !hasToken) {
  next('/login')  // 未登录则重定向到登录页
}
```

**状态**: ✅ 路由级别有保护，未登录无法访问 /datasets 页面

---

## 可能原因分析

### 原因排查表

| 问题 | 可能原因 | 检查方法 |
|------|----------|----------|
| Token 未存储 | 登录未成功或存储键名不一致 | 检查 localStorage 中 `access_token` 是否存在 |
| Token 未添加到请求 | 拦截器未执行或请求类型问题 | 打开浏览器 Network 面板检查请求头 |
| 后端未收到 Token | CORS 问题或网络问题 | 检查浏览器控制台网络错误 |
| Token 验证失败 | Token 过期或格式错误 | 检查后端日志中 JWT 验证错误 |
| 已登录但返回 null | 用户 ID 未正确存入 Token | 检查 JwtTokenService.getUserIdFromToken() |

---

## 快速诊断步骤

### 步骤 1: 检查浏览器 localStorage

在浏览器控制台执行:
```javascript
localStorage.getItem('access_token')
```

**预期结果**: 应该返回一个类似 `eyJhbGciOiJIUzI1NiIs...` 的 JWT 字符串  
**如果返回 null**: 说明用户未登录或登录未成功存储 Token

---

### 步骤 2: 检查浏览器 Network 面板

1. 打开浏览器开发者工具 (F12)
2. 切换到 Network 面板
3. 点击"仅创建"按钮发送请求
4. 找到 `datasets` 请求，检查:
   - **Request Headers** 中是否有 `Authorization: Bearer <token>`
   - **Response** 的状态码和内容

---

### 步骤 3: 检查后端日志

查看后端日志，确认:
- 请求是否到达后端
- JWT 验证是否成功
- 是否有异常抛出

---

## 根本原因分析

根据之前的诊断 (`debug-create-no-file.md`)，最可能的原因序列：

```
1. 前端发送请求（假设 Token 存在）
2. 后端 JWT 过滤器验证 Token
3. Token 有效 → 设置 Authentication 到 SecurityContext
4. Controller 处理请求
5. Service 调用 currentUserUtils.getCurrentUserId()
6. auth.getCredentials() 返回 userId
7. dataset.setCreatedBy(userId)
8. INSERT 到数据库 → created_by 违反 NOT NULL 约束
9. DataIntegrityViolationException (RuntimeException)
10. GlobalExceptionHandler 捕获 → 返回 "系统繁忙"
```

### 等等！如果 JWT 验证失败会怎样？

如果请求没有 Token 或 Token 无效：
1. JWT Filter 不设置 Authentication
2. Spring Security 的 `anyRequest().authenticated()` 拦截请求
3. 返回 401 Unauthorized

**预期行为**: 前端收到 401，响应拦截器显示 "登录已过期，请重新登录"

**实际行为**: 显示 "系统繁忙"

→ 说明请求确实到达了后端并进入了 Controller/Service，才抛出异常！

---

## 结论

1. **后端 Security 配置正确**: `/api/v1/datasets` 需要认证
2. **前端请求拦截器正确**: 会自动添加 Token
3. **前端路由守卫正确**: 未登录无法访问数据集页面
4. **最可能的问题**: Token 存在但用户 ID 获取失败

### 建议验证顺序

1. ✅ Browser Console: `localStorage.getItem('access_token')` 是否有值
2. ✅ Network Panel: 请求头中是否有 Authorization
3. ✅ Backend Logs: 确认请求到达和异常信息

如果 Token 和请求头都正常，问题可能在于:
- Token 中的 userId 为 null 或格式问题
- JwtTokenService.getUserIdFromToken() 获取不到正确的值
