# SecurityConfig 启动错误诊断报告

## 错误描述

应用启动时报错：
```
***************************
APPLICATION FAILED TO START
***************************

Description:

Field userDetailsService in com.gisplatform.config.SecurityConfig required a bean of type 'org.springframework.security.core.userdetails.UserDetailsService' that could not be found.

Action:

Consider defining a bean of type 'org.springframework.security.core.userdetails.UserDetailsService' in your application.
```

## 根因分析

`SecurityConfig.java` 中通过 `@Autowired` 注入了 `UserDetailsService`：

```java
@Autowired
private UserDetailsService userDetailsService;
```

但项目中**没有定义** `UserDetailsService` 的实现 Bean，导致 Spring 容器无法注入，启动失败。

## 需要创建的文件

### 1. InMemoryUserDetailsService.java（最小实现）

创建位置：`backend/src/main/java/com/gisplatform/security/InMemoryUserDetailsService.java`

```java
package com.gisplatform.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内存用户详情服务（测试用）
 * <p>
 * 使用硬编码的用户信息进行认证。
 * 生产环境应替换为数据库查询实现。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Service
public class InMemoryUserDetailsService implements UserDetailsService {

    /**
     * 测试用户账号
     * 用户名: admin
     * 密码: admin123 (BCrypt 加密)
     */
    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E"; // admin123
    private static final String TEST_ROLE = "ADMIN";

    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!TEST_USERNAME.equals(username)) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return User.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + TEST_ROLE)))
                .disabled(false)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .build();
    }

}
```

**说明**：
- 默认用户名：`admin`
- 默认密码：`admin123`（BCrypt 加密后存储）
- 角色：`ROLE_ADMIN`

### 2. 可选：密码生成工具

如果需要生成其他密码的 BCrypt hash，可以临时修改测试类的密码。

## 修复步骤

1. **创建 InMemoryUserDetailsService.java**
2. **重新启动应用**

```bash
cd backend
mvn spring-boot:run
```

3. **测试登录**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 架构演进

当前实现（测试用）：
```
UserDetailsService (InMemory)
         ↓
    硬编码用户
```

后续演进：
```
UserDetailsService (Jdbc)
         ↓
    数据库查询 (sys_user 表)
```

---

## 简化方案 vs 完整方案

| 方案 | 复杂度 | 适用场景 |
|------|--------|----------|
| 内存用户（当前） | 简单 | 开发/测试 |
| 数据库用户 | 中等 | 生产环境 |
| LDAP 用户 | 复杂 | 企业集成 |

## 其他需要补充的文件（后续迭代）

- `User` 实体类 → 对应 sys_user 表
- `UserMapper` → MyBatis-Plus Mapper
- `UserRoleMapper` → 用户角色关联
- 完整的登录 Controller

---

## 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | ADMIN |

修复后，应用应该能够正常启动并通过上述账号登录。
