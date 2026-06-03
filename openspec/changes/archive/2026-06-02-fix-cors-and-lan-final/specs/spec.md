本次变更为配置修改，不涉及规范层面的变更。

## 模块划分

- `backend/.../config/SecurityConfig.java` — CORS 配置修复
- `frontend/src/api/request.ts` — API 请求地址修改
- `frontend/vite.config.ts` — 验证配置正确

## 数据流设计

```
开发环境：
浏览器 → Vite (localhost:3000 或 192.168.x.x:3000) → 代理 /api/* → 后端 (localhost:8088)
```

## 接口列表

不变。前端通过 Vite 代理访问后端 API，前端代码无需修改具体调用方式。

## ADDED Requirements

（本次变更不引入新能力）
