# auth-api 认证接口规范

## 概述

提供用户认证 API，包括登录、注册、Token刷新、用户信息查询。

## 接口定义

### 1. 用户登录
- **URL**: POST /api/v1/auth/login
- **请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "xxx",
    "refreshToken": "yyy",
    "expiresIn": 900
  }
}
```

### 2. 用户注册
- **URL**: POST /api/v1/auth/register
- **请求体**:
```json
{
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "user@example.com"
}
```

### 3. 刷新Token
- **URL**: POST /api/v1/auth/refresh
- **请求体**:
```json
{
  "refreshToken": "xxx"
}
```

### 4. 获取用户信息
- **URL**: GET /api/v1/auth/userinfo
- **需要认证**: 是
- **响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "email": "admin@example.com",
    "phone": "13800138000"
  }
}
```

## 代码规范

- 所有类、方法使用中文 Javadoc 注释
- Controller 方法注释包含接口说明、请求参数、返回格式
