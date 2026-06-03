# 设计: fix-custom-user-details-service

## 当前问题

文件存在重复代码：
- 第 28-42 行：第一个 loadUserByUsername 方法（正确）
- 第 44-65 行：第一个 buildUserDetails 方法（正确）
- 第 69-102 行：重复的代码块（旧逻辑，查询角色功能被移除）

## 修复方案

删除重复代码，只保留一份完整的正确实现：

```java
/**
 * 自定义用户详情服务
 * <p>
 * 实现 Spring Security 的 UserDetailsService 接口，
 * 根据用户名从数据库查询用户信息并转换为 UserDetails 对象。
 * 同时查询用户角色信息，构建权限。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("用户名不能为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return buildUserDetails(user);
    }

    /**
     * 构建 UserDetails 对象
     *
     * @param user 用户实体
     * @return UserDetails 对象
     */
    private UserDetails buildUserDetails(User user) {
        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        
        List<SimpleGrantedAuthority> authorities;
        if (roleCodes == null || roleCodes.isEmpty()) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities = roleCodes.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
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
}
```

## 修改点

| 操作 | 说明 |
|------|------|
| 删除 | 第 69-102 行的重复代码 |
| 保留 | 第 1-67 行的正确实现 |
| 增强 | 添加完整的 Javadoc 注释 |
