# Proposal: fix-map-geojson-auth

## Summary

修复 MapContainer.vue 中 VectorSource 使用 url 加载 GeoJSON 导致 403 错误的问题。

## Problem Statement

控制台报错：GET /api/v1/datasets/2/geojson 返回 403 Forbidden

## Root Cause

VectorSource 使用原生 fetch 发起请求，不经过 axios 拦截器，导致请求未携带 JWT Token。

```typescript
// 当前代码
const source = new VectorSource({
  url: '/api/v1/datasets/${layerInfo.id}/geojson',  // fetch 无 JWT
  format: new GeoJSON()
})
```

## Goals

1. 将 VectorSource 的 url 替换为自定义 loader 函数
2. 手动从 localStorage 获取 JWT Token 并添加到请求头

## Success Criteria

- GeoJSON 请求返回 200 状态码
- 地图正确显示已加载的图层要素
