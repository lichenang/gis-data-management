## Context

影像管理页面 `views/images/index.vue` 已有"查看/发布/删除"操作按钮。后端已实现 `GET /api/v1/images/{id}/download-url` 返回预签名 URL。需要在前端添加下载入口。

## Goals / Non-Goals

**Goals:**
- `api/image.ts` 新增 `getImageDownloadUrl(id)` API 函数和对应 TypeScript 接口
- `views/images/index.vue` 操作列新增"下载原始影像"按钮
- 点击按钮 → 调用 API → 使用 `window.open(url)` 触发浏览器下载

**Non-Goals:**
- 不修改后端
- 不修改其他页面

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 下载方式 | `window.open(downloadUrl)` | 预签名 URL 是浏览器可识别的 HTTP GET 链接，直接打开即触发下载。前端无需追加隐藏 `<a>` 标签或使用 FileSaver |
| 按钮布局 | 放在"查看"和"发布"之间 | 操作频率：查看 > 下载 > 发布 > 删除，按使用频率排列 |
| 按钮显示条件 | 始终显示（不按状态过滤） | 无论 draft/published，只要数据存在就应可下载原始文件。后端会校验 type=raster |

```
用户点击"下载原始影像"
  → handleDownload(row)
     → getImageDownloadUrl(row.id)
        → GET /api/v1/images/{id}/download-url
           ← { downloadUrl, fileName, fileSize, expiresIn }
     → window.open(downloadUrl)
     → 浏览器下载 GeoTIFF
```

## Risks / Trade-offs

无风险。仅前端逻辑，后端 API 已稳定。
