## Context

`SecurityConfig.java` 中通过 `@Autowired` 注入了 `UserDetailsService`，但项目中没有该接口的实现 Bean，导致应用启动失败。

当前项目已有：
- `User` 实体类（对应 `sys_user` 表）
- `UserMapper`（MyBatis-Plus BaseMapper）
- `JwtAuthenticationFilter` 和 `JwtTokenService`
- Spring Security 基础配置

## Goals / Non-Goals

**Goals:**
- 创建 `CustomUserDetailsService` 实现 `UserDetailsService` 接口
- 注入 `UserMapper` 从数据库查询用户
- 将 `User` 实体转换为 Spring Security 的 `UserDetails`
- 解决应用启动时报错问题

**Non-Goals:**
- 不实现完整的用户角色管理（RBAC）
- 不修改现有 Jwt 相关代码
- 不实现多租户权限隔离

## Decisions

**方案选择：**
- **方案 A（内存实现）**：使用硬编码测试用户，简单快速但仅适用于开发测试
- **方案 B（数据库实现）**：使用 `UserMapper` 查询数据库，生产环境可用

**决定采用方案 B**：因为项目中已有 `User` 实体和 `UserMapper`，直接利用现有资源更合理。

**关键设计点：**
- 使用 `UserMapper.selectOne` 按用户名查询
- 使用 `User.builder()` 构建 `UserDetails`
- 从 token 中获取角色或预留角色字段

## Risks / Trade-offs

- [风险] `User` 实体暂未包含角色字段 → 用户可先使用默认角色登录，角色体系后续完善
- [风险] 密码使用 BCrypt 加密存储，需确保 User 实体中的 password 字段已加密
