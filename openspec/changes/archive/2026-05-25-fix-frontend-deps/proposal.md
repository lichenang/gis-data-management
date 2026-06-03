## Why

前端项目执行 `npm install` 报错：`No versions available for vite-plugin-gzip`。该 npm 包不存在（已下架/重命名），导致依赖安装失败，项目无法启动。

## What Changes

- 从 package.json 中移除不存在的 `vite-plugin-gzip` 依赖
- 移除未使用的 `vite-plugin-compression` 依赖（未在 vite.config.ts 中引用）
- 保留其他所有有效依赖

## Capabilities

### New Capabilities
- 无

### Modified Capabilities
- 无（仅修复依赖配置）

## Impact

- 修改文件：`frontend/package.json`

## Non-goals

- 不修改 vite.config.ts 配置
- 不添加其他新依赖
- 不修改业务代码
