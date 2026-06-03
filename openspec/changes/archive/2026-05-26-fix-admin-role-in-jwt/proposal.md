# fix-admin-role-in-jwt

## Why

根据诊断报告 `openspec/specs/debug-admin-role.md`，admin 用户登录后前端没有显示"用户管理"和"系统管理"菜单。问题是后端三处硬编码导致角色信息丢失：

1. CustomUserDetailsService 硬编码 `ROLE_USER`
2. AuthServiceImpl.login() 硬编码 `{"USER"}`
3. AuthServiceImpl.getUserInfo() 不返回 roles 字段

## What Changes

### 修复 1: CustomUserDetailsService
- 查询 `sys_user_role` 和 `sys_role` 表获取用户真实角色
- 使用角色 code 构建 `SimpleGrantedAuthority`

### 修复 2: AuthServiceImpl.login()
- 查询用户角色列表
- 将真实角色传入 JWT token

### 修复 3: AuthServiceImpl.getUserInfo()
- 查询用户角色
- 响应中添加 `roles` 字段

## Capabilities

### Fixed Capabilities
- 管理员登录后正确显示用户管理和系统管理菜单

## Impact

- 修改文件：
  - `backend/src/main/java/com/gisplatform/security/CustomUserDetailsService.java`
  - `backend/src/main/java/com/gisplatform/service/impl/AuthServiceImpl.java`

## Non-goals

- 不修改前端代码（前端逻辑已正确）
- 不修改其他业务逻辑
