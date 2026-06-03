## 1. 修改 Vite 开发服务器配置

- [x] 1.1 在 `vite.config.ts` 中添加 `server.host: '0.0.0.0'` 允许局域网访问
- [x] 1.2 在 `vite.config.ts` 中配置端口 `server.port: 3000`
- [x] 1.3 在 `vite.config.ts` 中配置代理，将 `/api` 请求代理到 `http://localhost:8088`

## 2. 验证

- [x] 2.1 启动前端开发服务器 `npm run dev`
- [x] 2.2 通过局域网 IP 访问前端，验证页面正常加载
- [x] 2.3 验证登录等功能正常工作
