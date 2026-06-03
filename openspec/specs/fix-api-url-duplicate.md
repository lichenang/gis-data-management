# Debug: API URL 重复拼接 /v1

## 问题描述

前端登录请求地址为 `/api/v1/v1/auth/login`，多了一个 `/v1` 片段。

Expected: `/api/v1/auth/login`  
Actual: `/api/v1/v1/auth/login`

## 根因分析

### URL 构造层级

```
Layer 1: request.ts (baseURL)
────────────────────────────────────────────────────────────
Line 7:
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088/api/v1'

Result: baseURL = "http://localhost:8088/api/v1"
```

```
Layer 2: store/user.ts (API path)
────────────────────────────────────────────────────────────
Line 68:
post('/v1/auth/login', loginParams)

Result: path = "/v1/auth/login"
```

```
Layer 3: Axios Request URL
────────────────────────────────────────────────────────────
Full URL = baseURL + path
         = "http://localhost:8088/api/v1" + "/v1/auth/login"
         = "http://localhost:8088/api/v1/v1/auth/login"
```

## 问题定位

### 根因：路径重复拼接

| 文件 | 行号 | 问题 |
|------|------|------|
| `frontend/src/api/request.ts` | 7 | baseURL 包含 `/api/v1` |
| `frontend/src/store/user.ts` | 68 | API 路径包含 `/v1` (应为 `/auth/login`) |
| `frontend/src/store/user.ts` | 83 | API 路径包含 `/v1` (应为 `/auth/logout`) |
| `frontend/src/store/user.ts` | 99 | API 路径包含 `/v1` (应为 `/auth/userinfo`) |

### 详细代码

**request.ts Line 7:**
```typescript
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088/api/v1'
```

**store/user.ts Line 68:**
```typescript
async function login(loginParams: LoginParams): Promise<void> {
  const result = await post<{ code: number; data: LoginResult }>('/v1/auth/login', loginParams)
  //                      ↑ 多余的 /v1
}
```

---

## 修复方案

### 方案一：修改 store/user.ts（推荐）

移除 API 路径中的 `/v1` 前缀，让 baseURL 处理版本号：

```typescript
// store/user.ts 修改
// Line 68: login
await post<{ code: number; data: LoginResult }>('/auth/login', loginParams)

// Line 83: logout
await post('/auth/logout')

// Line 99: getUserInfo
await get<{ code: number; data: UserInfo }>('/auth/userinfo')
```

### 方案二：修改 request.ts

移除 baseURL 中的 `/api/v1`，仅保留 host：

```typescript
// request.ts Line 7
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088'
//                                                  ↑ 移除 /api/v1
```

**注意**: 方案二会导致所有 API 调用都需要完整路径如 `/api/v1/auth/login`。

---

## 验证

修复后，登录请求应发送至：
- URL: `http://localhost:8088/api/v1/auth/login`
- 或通过 Vite 代理: `/api/v1/auth/login`

响应示例:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900
  }
}
```

---

## 结论

| 检查项 | 状态 |
|--------|------|
| request.ts baseURL 配置 | ✓ 包含 `/api/v1` |
| store/user.ts API 路径 | ❌ 多余 `/v1` 导致重复 |
| 根因 | 路径拼接重复 |
| 修复 | 移除 store/user.ts 中的 `/v1` 前缀 |
