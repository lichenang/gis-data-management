## 模块划分

| 模块 | 职责 | 关键文件 |
|------|------|---------|
| Axios 响应拦截器 | 统一处理所有 API 响应，区分 JSON 和 Blob 响应 | `request.ts` |
| 切片包下载 API | 封装 POST 请求，设置 `responseType: 'blob'` | `api/image.ts` `downloadTilePackage()` |

## 数据流设计

```
修正后的响应拦截器流程：

请求 → 请求拦截器 (添加 JWT Token)
  → Axios 发送
    → 后端返回 200 + ZIP 二进制
      → 响应拦截器:
        → 检查 response.config.responseType
          → 是 'blob'? → 直接 return response.data (Blob) ✅
          → 否 → 按 JSON 解析 (原有逻辑)
```

## 接口列表

无新增 API。仅修改前端响应拦截器内部逻辑。

## ADDED Requirements

### Requirement: 响应拦截器须支持 Blob 类型响应
响应拦截器 SHALL 在检测到请求配置 `responseType: 'blob'` 时跳过 JSON 解析，直接返回二进制数据。

#### Scenario: Blob 响应拦截成功返回
- **WHEN** `POST /api/v1/images/{id}/tile-package` 返回 HTTP 200，`Content-Type: application/zip`
- **THEN** 响应拦截器直接返回 `response.data`（Blob），不执行 JSON 解析

#### Scenario: Blob 响应不触发错误提示
- **WHEN** 后端返回 HTTP 200 + ZIP 二进制流
- **THEN** 前端不弹出"请求失败"等错误提示
