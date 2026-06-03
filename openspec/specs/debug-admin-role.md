# 诊断报告: 管理员角色不生效问题

## 问题描述

admin 用户在数据库中角色是 `ADMIN`（通过 `sys_user_role` 表关联 `role_id=1`，对应 `sys_role` 表 `code=ADMIN`），但登录后前端页面没有显示"用户管理"和"系统管理"菜单。

## 问题链路分析

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        完整认证链路                                      │
└─────────────────────────────────────────────────────────────────────────┘

数据库                    后端                              前端
───────                    ────                              ────
sys_user (admin)    ──┐
sys_role (ADMIN)    ──┼── CustomUserDetailsService ──► 硬编码 "ROLE_USER" ❌
sys_user_role       ──┘    loadUserByUsername()

                       │
                       ▼
                  AuthServiceImpl.login()
                       │
                       ▼
                  硬编码 roles = {"USER"} ❌
                  JWT claims: {roles: ["USER"]}

                       │
                       ▼
                  AuthServiceImpl.getUserInfo()
                       │
                       ▼
                  不返回 roles 字段 ❌

                       │
                       ▼
                  前端 /auth/userinfo
                       │
                       ▼
                  userInfo.roles = [] 空数组
                       │
                       ▼
                  isAdmin = roles.includes("ADMIN") = false ❌
```

---

## 发现的问题

### Issue #1: CustomUserDetailsService 硬编码角色

**文件**: `backend/src/main/java/com/gisplatform/security/CustomUserDetailsService.java`

**问题位置**: 第 66-68 行

```java
private UserDetails buildUserDetails(User user) {
    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_USER")  // ❌ 硬编码！
    );
    ...
}
```

**问题**: 没有查询 `sys_user_role` 和 `sys_role` 表，直接硬编码为 `ROLE_USER`

---

### Issue #2: AuthServiceImpl.login() 硬编码角色

**文件**: `backend/src/main/java/com/gisplatform/service/impl/AuthServiceImpl.java`

**问题位置**: 第 100 行

```java
String[] roles = {"USER"};  // ❌ 硬编码！
String accessToken = jwtTokenService.generateAccessToken(
        user.getUsername(),
        user.getId(),
        roles
);
```

**问题**: 没有查询用户实际角色，直接使用硬编码的 `{"USER"}`

---

### Issue #3: AuthServiceImpl.getUserInfo() 不返回角色

**文件**: `backend/src/main/java/com/gisplatform/service/impl/AuthServiceImpl.java`

**问题位置**: 第 208-231 行

```java
public Object getUserInfo() {
    ...
    Map<String, Object> result = new HashMap<>();
    result.put("id", user.getId());
    result.put("username", user.getUsername());
    result.put("nickname", user.getNickname());
    result.put("email", user.getEmail());
    result.put("phone", user.getPhone());
    result.put("avatar", user.getAvatar());
    // ❌ 没有返回 roles 字段！
    return result;
}
```

**问题**: 响应不包含 `roles` 字段，前端无法获取用户角色

---

## 数据库结构参考

```sql
-- 角色表
sys_role (
    id      BIGINT PRIMARY KEY,  -- 1 = ADMIN
    code    VARCHAR(64),          -- 'ADMIN'
    name    VARCHAR(64)           -- '管理员'
)

-- 用户角色关联表
sys_user_role (
    user_id BIGINT,   -- admin 用户 ID
    role_id BIGINT    -- 关联到 sys_role.id = 1
)
```

---

## 修复方案

### 修复 1: CustomUserDetailsService 查询角色

1. 注入 RoleMapper（或使用 UserMapper 查询关联）
2. 在 `buildUserDetails()` 中查询用户角色
3. 根据 `sys_role.code` 构建 `SimpleGrantedAuthority`

### 修复 2: AuthServiceImpl.login() 传递正确角色

1. 查询 `sys_user_role` 获取用户角色列表
2. 将角色列表传入 `generateAccessToken()`

### 修复 3: AuthServiceImpl.getUserInfo() 返回角色

1. 查询用户角色
2. 在响应中添加 `roles` 字段

---

## 建议修复任务

1. **修改 CustomUserDetailsService** - 从数据库加载真实角色
2. **修改 AuthServiceImpl.login()** - 使用数据库查询的角色
3. **修改 AuthServiceImpl.getUserInfo()** - 返回 roles 字段

## 相关文件

- `backend/src/main/java/com/gisplatform/security/CustomUserDetailsService.java`
- `backend/src/main/java/com/gisplatform/service/impl/AuthServiceImpl.java`
- `frontend/src/store/user.ts` (前端逻辑正确，只需后端返回 roles)
