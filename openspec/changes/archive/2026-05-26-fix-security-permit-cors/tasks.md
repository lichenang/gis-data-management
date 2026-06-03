## 1. 添加 HttpMethod import

- [x] 1.1 在 SecurityConfig.java 中添加 `import org.springframework.http.HttpMethod;`

## 2. 修改安全配置

- [x] 2.1 在 `authorizeHttpRequests` 方法中， `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()` 放在最前面
- [x] 2.2 确保原有的 permitAll 路径配置保持不变

## 3. 验证

- [ ] 3.1 重启后端服务
- [ ] 3.2 启动前端并登录
- [ ] 3.3 检查浏览器 Network 面板 OPTIONS 请求返回 200
- [ ] 3.4 确认登录成功获取 Token
- [ ] 3.5 访问 /users 页面验证用户列表正常
