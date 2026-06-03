## 模块划分

此变更仅影响前端影像管理模块。

| 模块 | 说明 |
|------|------|
| API 层 | `api/image.ts` 新增 API 函数 |
| 视图层 | `views/images/index.vue` 新增下载按钮和交互逻辑 |

## 数据流设计

```
  ┌─────────────┐    1. Click "下载原始影像"     ┌──────────────┐
  │   Vue Page  │ ──────────────────────────────▶│  image.ts    │
  │  index.vue  │                                │  API module  │
  │             │    2. GET /images/{id}/         │              │
  │             │       download-url              │              │
  │             │ ◀────────────────────────────── │              │
  │             │                                └──────┬───────┘
  │             │                                       │ HTTP
  │             │                                ┌──────▼───────┐
  │             │    3. window.open(url)         │   Backend    │
  │             │ ───────────────────────────────│  Spring Boot │
  │             │         (浏览器直接下载)        └──────────────┘
  └─────────────┘
```

## 接口列表

| 函数 | 参数 | 后端端点 |
|------|------|---------|
| `getImageDownloadUrl(id: number)` | `id` | `GET /api/v1/images/{id}/download-url` |

## ADDED Requirements

### Requirement: 影像列表页提供下载原始文件按钮
系统 SHALL 在影像管理列表的操作列提供"下载原始影像"按钮，点击后获取预签名 URL 并触发浏览器下载。

#### Scenario: 点击下载按钮
- **WHEN** 用户点击某行数据的"下载原始影像"按钮
- **THEN** 调用 `getImageDownloadUrl(id)` 获取预签名 URL，然后通过 `window.open(url)` 触发浏览器下载 GeoTIFF 文件

#### Scenario: 下载失败时提示错误
- **WHEN** API 调用失败（如数据集不存在）
- **THEN** 页面显示 ElMessage.error 提示错误信息
