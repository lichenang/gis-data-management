# 设计: fix-api-url-duplicate

## 问题定位

### 当前 URL 构造逻辑

```
request.ts (baseURL)     = "http://localhost:8088/api/v1"
store/user.ts (path)     = "/v1/auth/login"
                         ──────────────────────────────
最终 URL                 = "/api/v1/v1/auth/login"  ← 错误
```

### 需要修改的位置

文件: `frontend/src/store/user.ts`

| 行号 | 当前值 | 修改后 |
|------|--------|--------|
| 68 | `/v1/auth/login` | `/auth/login` |
| 83 | `/v1/auth/logout` | `/auth/logout` |
| 99 | `/v1/auth/info` | `/auth/userinfo` |

注意: 后端 AuthController 使用的是 `/userinfo` 而非 `/info`，需要保持一致。

## 验证步骤

1. 修改代码后重新构建前端
2. 访问登录页，使用 admin/admin123 登录
3. 确认请求发送至 `/api/v1/auth/login`
4. 确认返回 200 和 Token
