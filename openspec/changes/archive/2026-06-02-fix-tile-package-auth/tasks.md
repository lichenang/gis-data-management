## 1. 响应拦截器修复

- [x] 1.1 `request.ts` 响应拦截器中添加 `responseType: 'blob'` 检测，跳过 JSON 解析直接返回 Blob
