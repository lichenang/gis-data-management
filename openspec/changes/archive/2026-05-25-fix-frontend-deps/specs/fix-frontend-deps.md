# fix-frontend-deps 前端依赖修复规范

## 问题描述

npm install 报错，无法找到 vite-plugin-gzip 包。

## 修复内容

从 package.json 中移除：
- `vite-plugin-gzip` (不存在)
- `vite-plugin-compression` (未使用)

## 修复后验证

- npm install 执行成功
- npm run dev 可以启动开发服务器
