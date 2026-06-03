# 诊断报告: CORS 修复后所有 API 返回 403

## 问题描述

应用 `fix-cors-config` 变更后，所有 API（包括 `/api/v1/auth/login` 和 `/api/v1/users`）都返回 403 错误。

日志显示: `AnonymousAuthenticationFilter 直接拒绝了匿名请求`

---

## 当前 SecurityConfig 分析

### 1. permitAll() 配置 - ✓ 存在

当前配置（第 112-124 行）仍然保留了允许公开访问的路径：

```java
.requestMatchers(
    "/api/v1/auth/login",
    "/api/v1/auth/register", 
    "/api/v1/auth/refresh",
    "/doc.html",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/v3/api-docs",
    "/webjars/**",
    "/swagger-resources/**",
    "/actuator/**"
).permitAll()
```

**结论**: permitAll() 配置完好，没有被误改成 authenticated()

---

### 2. CORS 配置 - ✓ 正确

当前 CORS 配置（第 138-150 行）:

```java
configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"));
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
configuration.setMaxAge(3600L);
```

**结论**: CORS 配置本身是正确的

---

### 3. 真正的根因: 缺少 OPTIONS 放行

```
┌─────────────────────────────────────────────────────────────────────┐
│                    请求处理流程（问题所在）                          │
└─────────────────────────────────────────────────────────────────────┘

浏览器                    Spring Security                    处理结果
───────                   ──────────────                    ─────────

OPTIONS /api/v1/auth/login  ──▶  Security Filter Chain  ──▶   403 ✗
  (CORS 预检请求)                   │
                                   ▼
                          authorizeHttpRequests
                                   │
                    ┌──────────────┴──────────────┐
                    ▼                               ▼
            /api/v1/auth/login              anyRequest()
                 permitAll()                 authenticated()
                    │                               │
                    ▼                               ▼
               [不匹配] ⚠️                      拦截! 403
                    (因为是 OPTIONS 方法)
```

**问题根因**:

当浏览器发送 CORS 预检请求（OPTIONS 方法）时：

1. 请求 `OPTIONS /api/v1/auth/login` 进入 Security Filter Chain
2. `requestMatchers("/api/v1/auth/login")` 只匹配 GET/POST 等，**不匹配 OPTIONS**
3. 请求落到 `.anyRequest().authenticated()` → 需要认证
4. 没有携带 Token → AnonymousAuthenticationFilter 拒绝 → 403

---

## 解决方案

在 SecurityConfig 中添加 OPTIONS 预检请求的放行：

```java
.authorizeHttpRequests(auth -> auth
        // 新增: 放行所有 OPTIONS 请求（CORS 预检）
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        // 允许访问的路径
        .requestMatchers(
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/refresh",
                "/doc.html",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/v3/api-docs",
                "/webjars/**",
                "/swagger-resources/**",
                "/actuator/**"
        ).permitAll()
        // 其他请求需要认证
        .anyRequest().authenticated()
)
```

需要添加的 import:
```java
import org.springframework.http.HttpMethod;
```

---

## 为什么原来能工作？

原来的配置使用 `setAllowedOriginPatterns(List.of("*"))`:

```java
configuration.setAllowedOriginPatterns(List.of("*"));
```

这种方式在某些 Spring Security 版本中会隐式地允许 OPTIONS 请求，或者浏览器直接绕过 Spring Security 处理（因为通配符允许任何来源）。

---

## 验证步骤

1. 修改 SecurityConfig.java，添加 OPTIONS 放行
2. 重启后端服务
3. 使用前端发起登录请求
4. 检查浏览器 Network 面板:
   - 确认 OPTIONS 预检请求返回 200
   - 确认 POST 登录请求返回 200 + Token

---

## 相关文件

- `backend/src/main/java/com/gisplatform/config/SecurityConfig.java`

---

## 修复任务

创建新变更 `fix-cors-options`:
- 添加 `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()`
- 重新测试验证
