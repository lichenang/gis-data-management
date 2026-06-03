## 1. 创建 UserDetailsService 实现类

- [x] 1.1 创建 CustomUserDetailsService.java 文件
- [x] 1.2 实现 loadUserByUsername 方法，使用 UserMapper 查询用户
- [x] 1.3 实现 User 实体到 UserDetails 对象的转换
- [x] 1.4 添加用户状态检查逻辑（禁用/锁定）

## 2. 验证与测试

- [x] 2.1 编译项目验证代码正确性
- [x] 2.2 启动应用验证不再报 UserDetailsService 缺失错误
- [ ] 2.3 测试登录接口验证用户认证正常工作（需要数据库环境）
