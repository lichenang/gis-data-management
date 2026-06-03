# frontend-project Vue 3 前端项目规范

## 概述

GIS Platform 前端项目初始化规范，基于 Vue 3 + Vite + TypeScript + Element Plus。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 框架 |
| Vite | 5.x | 构建工具 |
| TypeScript | 5.x | 类型系统 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理 |
| Axios | 1.x | HTTP 客户端 |
| Element Plus | 2.x | UI 组件库 |
| OpenLayers | 10.x | 地图可视化 |

## 目录结构

```
src/
├── api/                 # API 接口封装
│   └── index.ts         # Axios 实例配置
├── assets/              # 静态资源
│   └── styles/          # 全局样式
├── components/          # 公共组件
├── layout/              # 布局组件
├── router/              # 路由配置
│   └── index.ts         # 路由定义
├── store/               # Pinia 状态管理
│   └── user.ts          # 用户状态
├── types/               # TypeScript 类型定义
├── utils/               # 工具函数
└── views/               # 页面组件
    ├── login/           # 登录页面
    └── home/            # 首页/仪表盘
```

## 核心配置

### Axios 配置
- baseURL: `http://localhost:8088/api/v1`
- 超时时间：30000ms
- 请求/响应拦截器
- JWT Token 自动携带

### 路由配置
- 静态路由：登录页、首页
- 动态路由：基于用户权限
- 路由守卫：认证检查

### 状态管理
- 用户状态：登录信息、Token、权限
- 应用状态：全局配置、菜单

## 注释规范

所有 .vue 文件使用 JSDoc 风格注释：
- 组件 Props 使用 @props 描述
- 组件 Emits 使用 @emits 描述
- 关键函数使用 @description 描述
- 注释语言：中文

## 验收标准

- 项目可成功启动 (npm run dev)
- TypeScript 编译无错误
- 所有依赖正确安装
- 目录结构符合规范
