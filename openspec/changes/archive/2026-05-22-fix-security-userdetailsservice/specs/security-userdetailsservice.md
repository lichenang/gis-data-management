# security-userdetailsservice 用户详情服务规范

## 概述

实现 `UserDetailsService` 接口，为 Spring Security 提供用户认证信息查询服务。

## 功能需求

- 实现 `UserDetailsService` 接口的 `loadUserByUsername(String username)` 方法
- 根据用户名从数据库查询用户信息
- 将查询到的用户信息转换为 Spring Security 的 `UserDetails` 对象
- 支持用户状态检查（禁用/锁定/过期）

## 技术实现

- 使用 `UserMapper` 查询用户
- 使用 Spring Security 的 `User.builder()` 构建返回对象
- 默认角色设置为 `ROLE_USER`，待角色体系完善后从数据库查询

## 验收标准

- 应用启动不再报错 `UserDetailsService` Bean 缺失
- 用户登录时可正确从数据库加载用户信息
- 密码验证使用 BCrypt 匹配
