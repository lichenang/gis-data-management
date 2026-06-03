## ADDED Requirements

### 模块划分

| 模块 | 位置 | 职责 |
|---|---|---|
| 数据集管理页面 | `frontend/src/views/datasets/index.vue` | 重写 `handleExport()` 函数，使用 Fetch + Blob 携带 JWT Token |

### 数据流设计

```
用户点击"导出 GeoJSON"
    │
    ▼
fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    │
    ├── Token 存在 ──→ GET /api/v1/datasets/{id}/export?format=geojson
    │                      │
    │                      ▼
    │                  Backend JwtAuthenticationFilter
    │                      │
    │                      ├── Token 有效 ──→ ExportController.exportDataset()
    │                      │                      │
    │                      │                      ▼
    │                      │                  Response: 文件流 → 200 OK
    │                      │
    │                      └── Token 无效 ──→ 401 Unauthorized
    │
    └── Token 不存在 ──→ 跳转登录页
```

### Requirement: 导出请求携带 JWT Token

导出的 GET 请求 SHALL 在 `Authorization` 请求头中携带 `Bearer <token>`，Token 从 `localStorage.getItem('access_token')` 读取。

#### Scenario: 带 Token 的导出请求成功

- **WHEN** 用户已登录且 Token 有效
- **AND** 用户点击导出按钮
- **THEN** 前端发送 `GET /api/v1/datasets/{id}/export?format={format}` 请求
- **AND** 请求包含 `Authorization: Bearer <token>` 请求头
- **AND** 后端返回 200 状态码和文件流
- **AND** 前端将响应内容作为文件下载

#### Scenario: Token 不存在或过期

- **WHEN** 用户未登录或 Token 已过期
- **AND** 用户点击导出按钮
- **THEN** 后端返回 401
- **AND** 前端应给出适当错误提示

### Requirement: 下载文件的文件名

下载的文件 SHALL 使用数据集名称作为文件名，使用导出格式作为扩展名。

#### Scenario: 文件名正确

- **WHEN** 导出 GeoJSON 格式的数据集名称为 "北京市道路"
- **THEN** 下载的文件名为 `北京市道路.geojson`
- **WHEN** 导出 Shapefile 格式的数据集名称为 "北京市道路"
- **THEN** 下载的文件名为 `北京市道路.shp.zip`
