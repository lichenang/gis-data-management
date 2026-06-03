## 1. 修复后端 CORS 配置

- [x] 1.1 检查 SecurityConfig.java 中 `corsConfigurationSource()` 方法确保正确配置
- [x] 1.2 确认 allowedOrigins 包含 `http://192.168.31.123:3000` 和 `http://localhost:*`
- [x] 1.3 确认 allowedMethods 包含 OPTIONS
- [x] 1.4 Maven 编译验证

## 2. 修复前端 API 请求地址

- [x] 2.1 修改 `src/api/request.ts` 中 baseURL 为 `/api`
- [x] 2.2 编译验证

## 3. 确认 Vite 配置

- [x] 3.1 确认 `vite.config.ts` 中 `server.host: '0.0.0.0'`
- [x] 3.2 确认 `/api` 代理配置指向 `http://localhost:8088`

## 4. 验证

- [ ] 4.1 启动后端服务
- [ ] 4.2 启动前端服务 `npm run dev`
- [ ] 4.3 通过局域网 IP 访问前端，验证登录功能正常
