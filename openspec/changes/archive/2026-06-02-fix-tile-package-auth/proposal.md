## Why

切片包下载接口 `POST /api/v1/images/{id}/tile-package` 返回 403 错误，导致用户无法下载切片包。实际根因是 `request.ts` 的响应拦截器将所有响应按 JSON 解析，但切片包下载接口因 `responseType: 'blob'` 返回二进制流，拦截器解构 Blob 对象得到 `undefined`，误判请求失败。

## What Changes

- 修复 `request.ts` 响应拦截器，对 `responseType === 'blob'` 的请求跳过 JSON 解析逻辑，直接返回 Blob
- 确保 `downloadTilePackage()` 中 `responseType: 'blob'` 能正确通过拦截器拿到 ZIP 二进制流
- 确保下载成功时触发浏览器下载，下载失败时正确显示后端返回的错误消息

## Capabilities

### New Capabilities
- （无）

### Modified Capabilities
- （无）

## Non-goals

- 不修改前端 `api/image.ts` 中的 `downloadTilePackage` 函数签名
- 不修改后端任何代码
- 不修改认证或授权逻辑

## Impact

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/api/request.ts` | 修改 | 响应拦截器增加 blob 类型检测，跳过 JSON 解析 |
