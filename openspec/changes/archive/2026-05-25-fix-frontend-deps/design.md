## Context

npm install 执行时报错：
```
npm ERR! 404 Not Found - ERROR: No versions available for vite-plugin-gzip
```

经核实的依赖状态：
- `vite-plugin-compression` v0.5.1 - 存在且可用，但 vite.config.ts 中未使用
- `vite-plugin-gzip` - **不存在**（npm 返回 404）

## Goals / Non-Goals

**Goals:**
- 移除不存在的包引用
- 移除未使用的包引用
- 确保 npm install 可以正常执行

**Non-goals:**
- 不添加新的 gzip 压缩方案（Nginx 层面处理）
- 不修改其他代码

## Decision

采用方案 A：直接移除两个 gzip 相关依赖

原因：
1. vite-plugin-gzip 不存在，必须移除
2. vite-plugin-compression 未在 vite.config.ts 中使用，可移除
3. 生产环境推荐使用 Nginx 配置 gzip 压缩，无需构建时处理
