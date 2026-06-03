## 1. 修改 CORS 配置

- [x] 1.1 修改 `corsConfigurationSource()` 方法
  - 将 `setAllowedOrigins()` 改为 `setAllowedOriginPatterns()`
  - 使用通配符模式：`["http://localhost:*", "http://127.0.0.1:*"]`

## 2. 验证

- [ ] 2.1 重启后端服务
- [ ] 2.2 启动前端服务（端口 3000）
- [ ] 2.3 在登录页面使用 admin/admin123 登录
- [ ] 2.4 检查浏览器 Network 面板 OPTIONS 返回 200
- [ ] 2.5 检查 POST 返回 200 + Token
- [ ] 2.6 确认成功跳转到首页
