# 设计: fix-security-permit-cors

## 问题定位

### 当前请求流程（有问题）

```
浏览器                          Spring Security                    结果
─────────────────────────────────────────────────────────────────────────

OPTIONS /api/v1/auth/login  →  requestMatchers(url)    →  不匹配! ⚠️
                                       │
                                       ▼
                              anyRequest().authenticated()
                                       │
                                       ▼
                                 需要认证 → 403 拒绝
```

### 修复后请求流程

```
浏览器                          Spring Security                    结果
─────────────────────────────────────────────────────────────────────────

OPTIONS /api/v1/auth/login  →  requestMatchers(OPTIONS)  →  匹配! ✓
                                       │
                                       ▼
                                 permitAll()  →  允许访问

GET /api/v1/auth/login      →  requestMatchers(url)      →  匹配! ✓
                                       │
                                       ▼
                                 permitAll()  →  允许访问
```

## 代码修改

### SecurityConfig.java

添加 import:
```java
import org.springframework.http.HttpMethod;
```

修改 `securityFilterChain` 方法，在 `authorizeHttpRequests` 开头添加：

```java
.authorizeHttpRequests(auth -> auth
        // 新增: 放行所有 OPTIONS 请求（CORS 预检）
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        // 原有配置保持不变
        .requestMatchers(...).permitAll()
        .anyRequest().authenticated()
)
```

### 修改位置

| 位置 | 修改内容 |
|------|----------|
| 第 25 行附近 | 添加 `import org.springframework.http.HttpMethod;` |
| 第 110-127 行 | 在 `authorizeHttpRequests` 开头添加 `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()` |

## 验证步骤

1. 重启后端服务
2. 前端发起登录请求
3. 浏览器 Network 面板检查：
   - OPTIONS 预检请求返回 200
   - POST 登录请求返回 200 + Token
4. 访问 /users 页面，验证用户列表正常加载
