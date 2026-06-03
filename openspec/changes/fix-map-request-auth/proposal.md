# Proposal: fix-map-request-auth

## Summary

修复地图查看页面获取已发布数据集列表时未携带 JWT Token 导致 403 问题。

## Problem Statement

在 `frontend/src/views/map/LayerPanel.vue` 中，使用原始 axios 发起 HTTP 请求，未通过配置好的请求拦截器，导致请求头中没有携带 JWT Token。后端返回 403 Forbidden。

## Root Cause Analysis

| 文件 | 问题代码 | 后果 |
|------|---------|------|
| `LayerPanel.vue` | `axios.get('/api/v1/datasets/published')` | 无 JWT Token → 403 |

正确做法：
- 使用 `@/api/request` 的封装方法 (如 `get()`)
- 或添加 API 方法到 `src/api/dataset.ts`

## Goals

1. 在 `src/api/dataset.ts` 新增获取已发布数据集列表的方法 `getPublishedDatasets`
2. 修改 `LayerPanel.vue` 使用配置好的 API 方法

## Success Criteria

- 地图页面能正常获取已发布数据集列表
- 请求携带 JWT Token，后端返回 200
- 左侧图层列表正常显示
