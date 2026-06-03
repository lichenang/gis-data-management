本次变更为配置修改，不涉及规范层面的变更。

## 模块划分

- `backend/.../config/CorsConfig.java` 或 `backend/.../config/SecurityConfig.java` — CORS 配置修改
- `backend/src/main/resources/application.yml` — CORS 配置属性

## 数据流设计

不变。CORS 预检请求在 Spring Security 过滤链之前处理。

## 接口列表

不变。不涉及 API 变更。

## ADDED Requirements

（本次变更不引入新能力）
