## 1. 修改 CORS 配置

- [x] 1.1 修改 `corsConfigurationSource()` 方法
  - 将 `setAllowedOriginPatterns(List.of("*"))` 改为 `setAllowedOrigins()`
  - 添加前端地址列表：`http://localhost:5173`, `http://127.0.0.1:5173`
  - 添加 `configuration.setMaxAge(3600L)` 设置预检缓存时间

## 2. 验证

- [ ] 2.1 重启后端服务
- [ ] 2.2 启动前端服务并登录
- [ ] 2.3 访问 /users 页面或调用 API
- [ ] 2.4 检查浏览器 Network 面板确认 CORS 响应头正确
- [ ] 2.5 确认不再返回 403 错误
