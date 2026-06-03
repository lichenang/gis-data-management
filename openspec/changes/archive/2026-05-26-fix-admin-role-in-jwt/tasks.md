## 1. 创建 RoleMapper

- [x] 1.1 创建 `RoleMapper.java` 接口
  - 位置: `mapper/RoleMapper.java`
  - 添加 `selectRoleCodesByUserId(Long userId)` 方法查询用户角色

## 2. 修改 CustomUserDetailsService

- [x] 2.1 注入 RoleMapper
- [x] 2.2 修改 `buildUserDetails()` 方法
  - 调用 RoleMapper 查询用户角色
  - 使用真实角色构建 authorities

## 3. 修改 AuthServiceImpl.login()

- [x] 3.1 注入 RoleMapper（如果未注入）
- [x] 3.2 修改 login() 方法
  - 查询用户角色列表
  - 传递真实角色到 JWT token

## 4. 修改 AuthServiceImpl.getUserInfo()

- [x] 4.1 修改 getUserInfo() 方法
  - 查询用户角色
  - 响应中添加 "roles" 字段

## 5. 验证

- [ ] 5.1 重启后端服务
- [ ] 5.2 使用 admin 账号登录
- [ ] 5.3 打印 JWT payload 检查 roles 字段
- [ ] 5.4 检查 /auth/userinfo 返回 roles
- [ ] 5.5 前端应显示"用户管理"菜单
