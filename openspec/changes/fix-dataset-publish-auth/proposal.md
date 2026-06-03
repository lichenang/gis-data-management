# Proposal: fix-dataset-publish-auth

## Summary

修复数据集发布/取消发布按钮使用裸 axios 导致请求未携带 JWT Token 的问题。

## Problem Statement

在 `datasets/index.vue` 中，发布/取消发布按钮使用了原始的 `axios` 库直接发起请求，没有通过配置好的 API 请求拦截器，导致请求头中没有携带 JWT Token。后端返回 403 Forbidden 错误。

## Root Cause

```typescript
// 当前代码 (问题代码):
import axios from 'axios'
await axios.put(`/api/v1/datasets/${row.id}/publish`)  // 无 JWT Token

// 应该使用配置好的 API 服务:
import { put } from '@/api/request'
await put(`/datasets/${row.id}/publish`)  // 通过拦截器自动添加 JWT Token
```

**区别**:
- 裸 `axios`: 直接发起请求，无任何拦截器处理
- `@/api/request`: 通过请求拦截器自动添加 `Authorization: Bearer <token>` 头

## Goals

1. 在 `src/api/dataset.ts` 中新增 `publishDataset` 和 `unpublishDataset` 方法
2. 在 `datasets/index.vue` 中使用配置好的 API 方法替换裸 axios 调用

## Success Criteria

- 点击发布/取消发布按钮能正常发送带有 JWT Token 的请求
- 后端返回 200 状态码，状态变更成功
- 列表自动刷新显示最新状态
