# 诊断报告: "仅创建" 按钮报错 "系统繁忙，请稍后再试"

## 问题描述

用户在不上传文件的情况下点击"仅创建"按钮时，收到错误提示：
> 系统繁忙，请稍后再试

## 问题定位

### 1. 错误响应来源

查看 `GlobalExceptionHandler.java` (第 194-199 行):

```java
@ExceptionHandler(RuntimeException.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
    logger.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
    return R.fail("系统繁忙，请稍后再试");  // <-- 这是返回的错误信息
}
```

所有未捕获的 `RuntimeException` 都会被捕获并返回通用的"系统繁忙"错误。

---

### 2. 根本原因分析

```
请求流程：
┌─────────────────────────────────────────────────────────────────────┐
│  1. 前端 POST /api/v1/datasets (JSON body)                         │
│                        │                                             │
│                        ▼                                             │
│  2. Spring Security 验证 JWT Token                                  │
│        ⚠️ 如果 Token 无效/过期/缺失 → 用户可能未认证                │
│                        │                                             │
│                        ▼                                             │
│  3. DatasetController.create()                                      │
│  4. DatasetServiceImpl.createDataset()                              │
│       │                                                              │
│       ▼                                                              │
│  5. Line 70: dataset.setCreatedBy(currentUserUtils.getCurrentUserId())
│                    │                                                  │
│                    ▼                                                  │
│         currentUserUtils.getCurrentUserId() 返回 null               │
│                    │                                                  │
│                    ▼                                                  │
│  6. dataset.setCreatedBy(null) ← created_by 被设为 null             │
│                        │                                             │
│                        ▼                                             │
│  7. this.save(dataset) → INSERT INTO dataset ...                   │
│                    │                                                  │
│                    ▼                                                 │
│  8. 数据库报错: null value in column "created_by" violates         │
│     NOT NULL constraint                                             │
│                    │                                                  │
│                    ▼                                                 │
│  9. 抛出 DataIntegrityViolationException (继承 RuntimeException)   │
│                    │                                                  │
│                    ▼                                                 │
│ 10. GlobalExceptionHandler 捕获 → 返回 "系统繁忙"                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 3. 代码确认

**CurrentUserUtils.java** (第 10-16 行):

```java
public Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        return null;  // ← 未认证时返回 null
    }
    return (Long) auth.getCredentials();
}
```

**Dataset 表结构** (数据库 migration):

```sql
-- V1__init_schema.sql, line 123
created_by          BIGINT NOT NULL,  ← 这里是 NOT NULL 约束
```

---

### 4. 可能原因

| 原因 | 说明 | 可能性 |
|------|------|--------|
| JWT Token 缺失 | 前端未携带 Token 请求 | 高 |
| JWT Token 过期 | Token 过期但前端仍发送请求 | 高 |
| Token 解析失败 | 后端 JWT 解析异常 | 低 |
| Security 配置问题 | `/api/v1/datasets` 需要认证但未正确配置 | 中 |

---

## 解决方案

### 方案 1: 改进全局异常处理（推荐）

在 `GlobalExceptionHandler` 中添加对 `DataIntegrityViolationException` 的专门处理：

```java
import org.springframework.dao.DataIntegrityViolationException;

// 添加新的异常处理方法
@ExceptionHandler(DataIntegrityViolationException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public R<Void> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
    String message = e.getMostSpecificCause().getMessage();
    
    // 检查是否是 created_by 相关错误
    if (message != null && message.contains("created_by")) {
        logger.warn("数据完整性约束违反 (created_by): {}", message);
        return R.fail(400, "创建失败：请确保已登录");
    }
    
    logger.warn("数据完整性约束违反: {}", message);
    return R.fail(400, "数据操作失败：" + message);
}
```

### 方案 2: 改进 UserId 获取逻辑

在 `DatasetServiceImpl.createDataset()` 中添加防御性检查：

```java
@Override
public boolean createDataset(Dataset dataset) {
    // ... 现有代码 ...
    
    Long userId = currentUserUtils.getCurrentUserId();
    if (userId == null) {
        throw new BusinessException(401, "用户未认证，无法创建数据集");
    }
    dataset.setCreatedBy(userId);
    
    return this.save(dataset);
}
```

### 方案 3: 添加更详细的前端错误显示

当前端收到错误响应时，应该显示具体的错误信息而不是通用的"系统繁忙"。

检查 `frontend/src/api/request.ts` 的响应拦截器：

```typescript
service.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200 || code === 0) {
      return response.data
    }
    // 这里应该显示后端返回的 message
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || 'Request failed'))
  },
  (error) => {
    // 这里是网络错误的情况
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)  // 实际错误应该能显示
  }
)
```

---

## 推荐行动

1. **首先**: 添加 `DataIntegrityViolationException` 处理器，获取更明确的错误信息
2. **其次**: 在 Service 层添加用户认证检查，给出明确提示
3. **同时**: 检查前端请求是否正确携带 JWT Token

## 测试建议

1. 登录后测试创建流程
2. 检查 Network 面板，确认请求头带有 `Authorization: Bearer <token>`
3. 后端日志应显示具体的数据库异常堆栈
