# 设计: fix-admin-role-in-jwt

## 问题链路

```
数据库                    后端                              前端
───────                    ────                              ────
sys_user (admin)    ──┐
sys_role (ADMIN)    ──┼── CustomUserDetailsService ──► 硬编码 "ROLE_USER" ❌
sys_user_role       ──┘

                       │
                       ▼
                  AuthServiceImpl.login()
                       │
                       ▼
                  硬编码 roles = {"USER"} ❌

                       │
                       ▼
                  AuthServiceImpl.getUserInfo()
                       │
                       ▼
                  不返回 roles 字段 ❌

                       │
                       ▼
                  前端 roles = [] → isAdmin = false ❌
```

## 修复 1: CustomUserDetailsService

### 当前代码 (第 66-68 行)

```java
List<SimpleGrantedAuthority> authorities = Collections.singletonList(
    new SimpleGrantedAuthority("ROLE_USER")
);
```

### 修改方案

需要查询用户角色，使用 MyBatis-Plus 的子查询或直接使用 RoleMapper：

```java
private UserDetails buildUserDetails(User user) {
    // 查询用户角色
    List<String> roleCodes = getUserRoleCodes(user.getId());
    
    List<SimpleGrantedAuthority> authorities = roleCodes.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toList());
    
    if (authorities.isEmpty()) {
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(user.getStatus() == 0)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .build();
}

private List<String> getUserRoleCodes(Long userId) {
    // 使用 RoleMapper 查询
    List<Role> roles = roleMapper.selectRolesByUserId(userId);
    return roles.stream()
            .map(Role::getCode)
            .collect(Collectors.toList());
}
```

## 修复 2: AuthServiceImpl.login()

### 当前代码 (第 100 行)

```java
String[] roles = {"USER"};
```

### 修改方案

```java
// 查询用户角色
List<String> userRoles = getUserRoles(user.getId());
String[] roles = userRoles.isEmpty() ? new String[]{"USER"} 
                                    : userRoles.toArray(new String[0]);

String accessToken = jwtTokenService.generateAccessToken(
        user.getUsername(),
        user.getId(),
        roles
);
```

## 修复 3: AuthServiceImpl.getUserInfo()

### 当前代码 (缺少 roles 字段)

```java
Map<String, Object> result = new HashMap<>();
result.put("id", user.getId());
result.put("username", user.getUsername());
result.put("nickname", user.getNickname());
result.put("email", user.getEmail());
result.put("phone", user.getPhone());
result.put("avatar", user.getAvatar());
```

### 修改方案

```java
// 查询用户角色
List<String> userRoles = getUserRoles(user.getId());

Map<String, Object> result = new HashMap<>();
result.put("id", user.getId());
result.put("username", user.getUsername());
result.put("nickname", user.getNickname());
result.put("email", user.getEmail());
result.put("phone", user.getPhone());
result.put("avatar", user.getAvatar());
result.put("roles", userRoles);  // 新增

return result;
```

## 需要新增的 Mapper

### RoleMapper.java

```java
@Mapper
public interface RoleMapper {
    @Select("SELECT r.code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);
}
```

或者在 UserMapper 中添加关联查询方法。

## 验证方式

1. 使用 admin 账号登录
2. 检查 JWT payload 中的 roles 字段是否为 ["ADMIN"]
3. 检查 /auth/userinfo 返回的 roles 字段
4. 登录后前端应显示"用户管理"菜单
