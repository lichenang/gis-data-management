## Why

应用启动时报错：SecurityConfig 需要 `UserDetailsService` Bean 但缺失，导致 Spring 容器无法注入，启动失败。项目中已有 `JwtAuthenticationFilter` 和 `JwtTokenService`，但缺少 `UserDetailsService` 实现类来满足 Security 配置需求。

## What Changes

- 创建 `CustomUserDetailsService` 实现 `UserDetailsService` 接口
- 使用 `UserMapper` 从数据库查询用户（方案 A：内存实现快速修复，方案 B：数据库实现生产可用）
- 将用户信息转换为 Spring Security 的 `UserDetails` 对象
- 支持基于用户名查询并返回用户角色权限

## Capabilities

### New Capabilities
- `security-userdetailsservice`: 实现 `UserDetailsService` 接口，提供用户认证信息查询服务

### Modified Capabilities
- 无

## Impact

- 新增文件：`backend/src/main/java/com/gisplatform/security/CustomUserDetailsService.java`
- 受影响配置：`SecurityConfig.java` 中的 `UserDetailsService` 注入将成功

## Non-goals

- 不实现完整的用户角色管理功能
- 不实现数据库用户表结构变更（User 实体已存在）
- 不修改现有的 JwtAuthenticationFilter 和 JwtTokenService
