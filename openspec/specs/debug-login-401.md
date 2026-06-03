# Debug: 登录接口返回 401 用户名或密码错误

## 问题描述

- 登录接口 `/api/v1/auth/login` 返回 401 "用户名或密码错误"
- 请求已成功到达 AuthController（排除路由问题）
- 使用测试账号 admin 登录失败

## 根因分析

### 1. 数据库存储的密码Hash

文件: `backend/src/main/resources/db/migration/V2__init_data.sql`

```sql
-- Line 69-72
-- Password: admin123 (BCrypt encrypted)
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E
INSERT INTO sys_user (username, password, ...) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', ...);
```

### 2. 用户输入

| 用户输入 | `Admin@123456` |
|----------|----------------|
| 数据库对应 | `admin123` |

**结论**: 密码不匹配

### 3. 验证逻辑（代码正确）

`AuthServiceImpl.login()` 使用 Spring Security 进行认证:

```java
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        loginRequest.getUsername(),
        loginRequest.getPassword()
    )
);
```

- `AuthenticationManager` → `DaoAuthenticationProvider` → `BCryptPasswordEncoder.matches()`

代码逻辑正确，问题在于输入密码与存储的 hash 不对应。

---

## 修复方案

### 方案一：更新用户输入（简单，推荐用于测试）

使用正确的密码登录：

```
username: admin
password: admin123   ← 而非 Admin@123456
```

### 方案二：更新数据库密码Hash（推荐用于统一测试账号）

生成 `Admin@123456` 的 BCrypt hash 并更新数据库：

```bash
# 使用 Spring Boot 或在线工具生成 BCrypt hash
# Password: Admin@123456
# BCrypt: $2a$10$XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

然后更新 SQL 迁移脚本 `V2__init_data.sql`：

```sql
-- 更新 admin 密码
UPDATE sys_user SET password = '$2a$10$新生成的hash' WHERE username = 'admin';
```

或者创建新的迁移脚本 `V3__update_admin_password.sql`：

```sql
-- Flyway Migration: V3__update_admin_password.sql
UPDATE sys_user SET password = '$2a$10$新hash' WHERE username = 'admin';
```

### 方案三：保持一致性（在 proposal 中统一测试密码）

如果希望测试账号统一使用 `Admin@123456`，执行以下步骤：

1. 生成 BCrypt hash
2. 更新 V2__init_data.sql 中的初始密码 hash
3. 重新初始化数据库（或创建迁移脚本）
4. 文档中统一说明测试账号密码

---

## 验证步骤

1. 使用正确密码 `admin123` 登录
2. 应返回:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900,
    "userId": 1,
    "username": "admin"
  }
}
```

---

## 结论

| 检查项 | 状态 |
|--------|------|
| AuthServiceImpl 密码验证逻辑 | ✓ 正确 |
| BCryptPasswordEncoder 注入 | ✓ 正确 |
| 数据库密码Hash格式 | ✓ 正确（BCrypt） |
| 密码不匹配 | ❌ 根因 |

**修复**: 使用密码 `admin123` 登录，或更新数据库 hash 为 `Admin@123456` 的 BCrypt 值。
