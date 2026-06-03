# fix-custom-user-details-service

## Why

当前 `CustomUserDetailsService.java` 文件存在以下问题：
1. 存在重复的 `loadUserByUsername` 和 `buildUserDetails` 方法定义
2. 第二个 `buildUserDetails` 方法使用了旧的硬编码逻辑，没有查询真实角色
3. 导致角色查询功能失效，admin 用户无法获取正确角色

## What Changes

完全重写 `CustomUserDetailsService.java`：
1. 注入 UserMapper 和 RoleMapper
2. loadUserByUsername：根据用户名查询用户，调用 buildUserDetails
3. buildUserDetails：查询真实角色，映射为 ROLE_XXX 格式
4. 添加完整的 Javadoc 注释
5. 删除所有重复代码

## Impact

- 修改文件：`backend/src/main/java/com/gisplatform/security/CustomUserDetailsService.java`
- 影响：用户认证时能获取真实角色

## Non-goals

- 不修改其他文件
