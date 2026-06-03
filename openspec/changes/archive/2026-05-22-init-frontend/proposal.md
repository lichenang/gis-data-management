## Why

项目后端已就绪，需要初始化 Vue 3 + Vite 前端项目以实现地图可视化和用户交互。前端是 GIS Platform 系统的重要组成部分，需要与后端 API 无缝集成。

## What Changes

- 创建 Vue 3 + Vite 前端项目骨架
- 集成 Vue Router（路由管理）
- 集成 Pinia（状态管理）
- 集成 Axios（HTTP 请求），配置 baseURL 为 http://localhost:8088/api/v1
- 集成 Element Plus（UI 组件库）
- 集成 OpenLayers（地图可视化）
- 配置 Knife4j 前端文档访问
- 创建标准目录结构：views、components、router、store、api、assets
- 所有 .vue 文件使用 TypeScript + Composition API
- 注释遵循 JSDoc 风格

## Capabilities

### New Capabilities
- `frontend-project`: Vue 3 前端项目初始化

### Modified Capabilities
- 无

## Impact

- 新增目录：`frontend/`
- 新增配置文件：package.json、vite.config.ts、tsconfig.json 等
- 创建目录结构：views、components、router、store、api、assets

## Non-goals

- 不实现具体业务页面（后续迭代）
- 不连接真实后端 API（仅配置占位符）
- 不实现复杂地图交互（后续迭代）
