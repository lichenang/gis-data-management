本次变更为配置修改，不涉及规范层面的变更。

## 模块划分

- `frontend/vite.config.ts` — Vite 开发服务器和代理配置
- `frontend/src/api/request.ts` — 无需修改（代理方案）

## 数据流设计

```
浏览器 → Vite 开发服务器 (localhost:3000 或 192.168.x.x:3000)
           ↓ 代理 /api/* 请求
        后端 (localhost:8088)
```

## 接口列表

不变。前端通过代理访问后端 API，前端代码无需修改 API 调用方式。

## ADDED Requirements

（本次变更不引入新能力）
